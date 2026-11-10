package com.app.subly.service;

import com.app.subly.dto.subscription.*;
import com.app.subly.dto.subscription.PaymentDetails;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.exception.subscription.MembershipActionForbiddenException;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.exception.subscription.PriceTooLowException;
import com.app.subly.exception.subscription.SubscriptionMemberNotFoundException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.model.user.User;
import com.app.subly.repository.PendingInvitationRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAccess subscriptionAccess;
    private final SubscriptionMembershipRepository membershipRepository;
    private final SubscriptionProviderRepository subscriptionProviderRepository;
    private final UserRepository userRepository;
    private final SubscriptionShares subscriptionShares;
    private final PendingInvitationRepository pendingInvitationRepository;
    private final Clock clock;

    @Transactional
    public SubscriptionResponse create(String username, CreateSubscriptionRequest request) {
        User owner = userRepository.getByUsername(username);
        requireNotInPast(owner, request);
        requireCentPerParticipant(request.price(), request.maxParticipants());
        SubscriptionProvider provider = findVisibleProvider(request.providerId(), owner);
        return subscribe(owner, provider, request, request.maxParticipants());
    }

    @Transactional(readOnly = true)
    public List<VisibleSubscriptionResponse> findCurrent(String username) {
        return membershipRepository
                .findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        username, SubscriptionStatus.ACTIVE)
                .stream()
                .map(VisibleSubscriptionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CancelledSubscriptionResponse> findCancelled(String username) {
        LocalDate today = userRepository.getByUsername(username).today(clock);
        return subscriptionShares.load(username, SubscriptionStatus.CANCELLED).stream()
                .map(share -> CancelledSubscriptionResponse.from(share.membership(), share.savedUntil(today)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionMemberResponse> findMembers(String ownerUsername, long subscriptionId) {

        subscriptionAccess.getOwnedSubscription(ownerUsername, subscriptionId);

        return membershipRepository.findBySubscriptionIdOrderByIdAsc(subscriptionId).stream()
                .map(SubscriptionMemberResponse::from)
                .toList();
    }

    @Transactional
    public SubscriptionResponse update(String username, long subscriptionId, UpdateSubscriptionRequest request) {
        SubscriptionMembership owner =
                subscriptionAccess.getOwnerMembershipWithSubscriptionDetails(username, subscriptionId);
        Subscription subscription = owner.getSubscription();
        requireNotInPast(owner.getUser(), request);
        requireCentPerParticipant(request.price(), subscription.getMaxParticipants());
        applyPaymentDetails(subscription, request);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public void advancePaymentDates(ZoneId zone, LocalDate today) {
        membershipRepository
                .findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                        MembershipRole.OWNER, SubscriptionStatus.ACTIVE, zone, today)
                .forEach(owner -> owner.getSubscription().advancePaymentDate(today));
    }

    @Transactional
    public void delete(String username, long subscriptionId) {
        Subscription subscription = subscriptionAccess.getOwnedSubscription(username, subscriptionId);

        //  Decide what happens to payments when they are implemented.
        pendingInvitationRepository.deleteBySubscriptionId(subscriptionId);
        membershipRepository.deleteBySubscription_IdIn(List.of(subscriptionId));
        membershipRepository.flush();
        subscriptionRepository.delete(subscription);
        subscriptionRepository.flush();
    }

    @Transactional
    public void cancel(String username, long subscriptionId) {
        Subscription subscription = subscriptionAccess.getOwnedSubscription(username, subscriptionId);

        if (!subscription.isActive()) {
            return;
        }

        subscription.cancel(Instant.now(clock));
    }

    @Transactional
    public void reactivate(String username, long subscriptionId) {
        SubscriptionMembership owner = subscriptionAccess.getOwnerMembership(username, subscriptionId);
        Subscription subscription = owner.getSubscription();

        if (subscription.isActive()) {
            return;
        }

        LocalDate today = owner.getUser().today(clock);

        subscription.reactivate(today);
    }

    @Transactional
    public void leave(String username, long subscriptionId) {

        SubscriptionMembership membership = membershipRepository
                .findBySubscriptionIdAndUserUsername(subscriptionId, username)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        if (membership.getRole() != MembershipRole.MEMBER) {
            throw new MembershipActionForbiddenException("The owner cannot leave their own subscription");
        }

        membershipRepository.delete(membership);
    }

    @Transactional
    public void removeMember(String ownerUsername, long subscriptionId, long membershipId) {

        subscriptionAccess.getOwnedSubscription(ownerUsername, subscriptionId);

        SubscriptionMembership target = membershipRepository
                .findById(membershipId)
                .filter(membership -> membership.getSubscription().getId().equals(subscriptionId))
                .orElseThrow(() -> new SubscriptionMemberNotFoundException(membershipId));

        if (target.getRole() != MembershipRole.MEMBER) {
            throw new MembershipActionForbiddenException("The owner cannot be removed from the subscription");
        }

        membershipRepository.delete(target);
    }

    @Transactional
    public void deleteAllSubscriptionsFor(Long userId) {
        List<Long> subscriptionIds = membershipRepository.findByUser_IdAndRole(userId, MembershipRole.OWNER).stream()
                .map(membership -> membership.getSubscription().getId())
                .toList();

        membershipRepository.deleteByUser_Id(userId);

        if (subscriptionIds.isEmpty()) {
            return;
        }

        subscriptionRepository.deleteAllById(subscriptionIds);
    }

    private SubscriptionResponse subscribe(
            User owner, SubscriptionProvider provider, PaymentDetails request, int maxParticipants) {
        Subscription subscription = new Subscription(
                provider, request.price(), request.billingCycle(), request.nextPaymentDate(), request.freeTrial());

        if (maxParticipants > 1) {
            subscription.makeSharable(maxParticipants);
        }
        subscriptionRepository.save(subscription);

        membershipRepository.save(SubscriptionMembership.owner(owner, subscription));
        return SubscriptionResponse.from(subscription);
    }

    private SubscriptionProvider findVisibleProvider(long providerId, User owner) {
        return subscriptionProviderRepository
                .findById(providerId)
                .filter(provider -> provider.isVisibleTo(owner))
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
    }

    private void requireNotInPast(User user, PaymentDetails request) {
        if (request.nextPaymentDate().isBefore(user.today(clock))) {
            throw new PaymentDateInPastException(request.nextPaymentDate());
        }
    }

    private static void requireCentPerParticipant(BigDecimal price, int maxParticipants) {
        BigDecimal minimum = BigDecimal.valueOf(maxParticipants, Subscription.PRICE_FRACTION_DIGITS);
        if (price.compareTo(minimum) < 0) {
            throw new PriceTooLowException(price, maxParticipants, minimum);
        }
    }

    private static void applyPaymentDetails(Subscription subscription, PaymentDetails request) {
        subscription.setPrice(request.price());
        subscription.setBillingCycle(request.billingCycle());
        subscription.reschedule(request.nextPaymentDate());
        subscription.setFreeTrial(request.freeTrial());
    }
}
