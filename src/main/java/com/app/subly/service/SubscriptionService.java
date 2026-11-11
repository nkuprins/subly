package com.app.subly.service;

import com.app.subly.dto.subscription.ActiveSubscriptionResponse;
import com.app.subly.dto.subscription.CancelledSubscriptionResponse;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.entity.user.User;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.exception.subscription.PriceTooLowException;
import com.app.subly.exception.subscription.SubscriptionNotActiveException;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.util.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
    private final SubscriptionProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final SubscriptionShares subscriptionShares;
    private final Clock clock;

    @Transactional
    public void create(String username, CreateSubscriptionRequest request) {
        User owner = userRepository.getByUsername(username);
        requireNotInPast(owner, request.nextPaymentDate());
        requireCentPerParticipant(request.price(), request.maxParticipants());
        SubscriptionProvider provider = findVisibleProvider(request.providerId(), owner);
        Subscription subscription = new Subscription(
                provider, request.price(), request.billingCycle(), request.nextPaymentDate(), request.freeTrial());
        if (request.maxParticipants() > 1) {
            subscription.makeSharable(request.maxParticipants());
        }
        subscriptionRepository.save(subscription);
        membershipRepository.save(SubscriptionMembership.owner(owner, subscription));
    }

    @Transactional(readOnly = true)
    public List<ActiveSubscriptionResponse> findActive(String username) {
        return subscriptionShares.forUser(username, SubscriptionStatus.ACTIVE).stream()
                .map(share -> ActiveSubscriptionResponse.from(share.membership(), share.amount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getActive(String username, long subscriptionId) {
        Subscription subscription =
                subscriptionAccess.getMembership(username, subscriptionId).getSubscription();
        if (!subscription.isActive()) {
            throw new SubscriptionNotActiveException(subscriptionId);
        }
        return SubscriptionResponse.from(subscription);
    }

    @Transactional(readOnly = true)
    public List<CancelledSubscriptionResponse> findCancelled(String username) {
        LocalDate today = userRepository.getByUsername(username).today(clock);
        return subscriptionShares.forUser(username, SubscriptionStatus.CANCELLED).stream()
                .map(share -> CancelledSubscriptionResponse.from(share.membership(), share.savedUntil(today)))
                .toList();
    }

    @Transactional
    public void update(String username, long subscriptionId, UpdateSubscriptionRequest request) {
        SubscriptionMembership owner = subscriptionAccess.getOwnerMembership(username, subscriptionId);
        Subscription subscription = owner.getSubscription();
        requireNotInPast(owner.getUser(), request.nextPaymentDate());
        requireCentPerParticipant(request.price(), subscription.getMaxParticipants());
        subscription.changePaymentDetails(
                request.price(), request.billingCycle(), request.nextPaymentDate(), request.freeTrial());
    }

    @Transactional
    public void delete(String username, long subscriptionId) {
        Subscription subscription = subscriptionAccess.getOwnedSubscription(username, subscriptionId);
        subscriptionRepository.delete(subscription);
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
        subscription.reactivate(owner.getUser().today(clock));
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

    private SubscriptionProvider findVisibleProvider(long providerId, User owner) {
        return providerRepository
                .findById(providerId)
                .filter(provider -> provider.isVisibleTo(owner))
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
    }

    private void requireNotInPast(User user, LocalDate nextPaymentDate) {
        if (nextPaymentDate.isBefore(user.today(clock))) {
            throw new PaymentDateInPastException(nextPaymentDate);
        }
    }

    private static void requireCentPerParticipant(BigDecimal price, int maxParticipants) {
        BigDecimal minimum = BigDecimal.valueOf(maxParticipants, Money.FRACTION_DIGITS);
        if (price.compareTo(minimum) < 0) {
            throw new PriceTooLowException(price, maxParticipants, minimum);
        }
    }
}
