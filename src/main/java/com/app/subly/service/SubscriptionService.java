package com.app.subly.service;

import com.app.subly.dto.PaymentDetails;
import com.app.subly.dto.subscription.*;
import com.app.subly.exception.subscription.CatalogProviderNotFoundException;
import com.app.subly.exception.subscription.CatalogProviderReadOnlyException;
import com.app.subly.exception.subscription.CategoryNotFoundException;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.model.User;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.ProviderType;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionProvider;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAccess subscriptionAccess;
    private final SubscriptionMembershipRepository membershipRepository;
    private final SubscriptionProviderRepository subscriptionProviderRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public SubscriptionResponse create(String username, CreateSubscriptionRequest request) {
        User owner = findUser(username);
        requireNotInPast(owner, request);
        SubscriptionProvider provider = findCatalogProvider(request.providerId());
        return subscribe(owner, provider, request);
    }

    @Transactional
    public SubscriptionResponse createCustom(String username, CreateCustomSubscriptionRequest request) {
        User owner = findUser(username);
        requireNotInPast(owner, request);
        Category category = findCategory(request.categoryId());
        SubscriptionProvider provider = subscriptionProviderRepository.save(
                SubscriptionProvider.custom(request.name().strip(), category, owner));
        return subscribe(owner, provider, request);
    }

    @Transactional(readOnly = true)
    public List<VisibleSubscriptionResponse> findVisibleTo(String username) {
        return membershipRepository
                .findByUser_UsernameAndSubscription_StatusOrderBySubscription_NextPaymentDateAsc(
                        username, SubscriptionStatus.ACTIVE)
                .stream()
                .map(VisibleSubscriptionResponse::from)
                .toList();
    }

    @Transactional
    public SubscriptionResponse update(String username, long subscriptionId, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionAccess.getOwnedSubscription(username, subscriptionId);
        requireNotInPast(findUser(username), request);
        applyPaymentDetails(subscription, request);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse updateCustom(
            String username, long subscriptionId, UpdateCustomSubscriptionRequest request) {
        Subscription subscription = subscriptionAccess.getOwnedSubscription(username, subscriptionId);
        SubscriptionProvider provider = subscription.getProvider();
        if (provider.getProviderType() != ProviderType.CUSTOM) {
            throw new CatalogProviderReadOnlyException(provider.getName());
        }
        requireNotInPast(findUser(username), request);
        Category category = findCategory(request.categoryId());
        provider.setName(request.name().strip());
        provider.setCategory(category);
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
        SubscriptionProvider provider = subscription.getProvider();

        //  Decide what happens to payments when they are implemented.
        subscriptionRepository.delete(subscription);
        if (provider.getProviderType() == ProviderType.CUSTOM) {
            subscriptionProviderRepository.delete(provider);
        }
    }

    @Transactional
    public void cancel(String username, long subscriptionId) {
        Subscription subscription = subscriptionAccess.getOwnedSubscription(username, subscriptionId);

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            return;
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now(clock));
    }

    @Transactional
    public void reactivate(String username, long subscriptionId) {
        Subscription subscription = subscriptionAccess.getOwnedSubscription(username, subscriptionId);

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        LocalDate today = findUser(username).today(clock);

        subscription.advancePaymentDate(today);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelledAt(null);
    }

    private SubscriptionResponse subscribe(User owner, SubscriptionProvider provider, PaymentDetails request) {
        Subscription subscription = new Subscription(
                provider, request.price(), request.billingCycle(), request.nextPaymentDate(), request.freeTrial());
        subscriptionRepository.save(subscription);

        membershipRepository.save(SubscriptionMembership.owner(owner, subscription));
        return SubscriptionResponse.from(subscription);
    }

    private User findUser(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User " + username + " does not exist"));
    }

    private SubscriptionProvider findCatalogProvider(long providerId) {
        return subscriptionProviderRepository
                .findByIdAndProviderType(providerId, ProviderType.CATALOG)
                .orElseThrow(() -> new CatalogProviderNotFoundException(providerId));
    }

    private Category findCategory(long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private void requireNotInPast(User user, PaymentDetails request) {
        if (request.nextPaymentDate().isBefore(user.today(clock))) {
            throw new PaymentDateInPastException(request.nextPaymentDate());
        }
    }

    private static void applyPaymentDetails(Subscription subscription, PaymentDetails request) {
        subscription.setPrice(request.price());
        subscription.setBillingCycle(request.billingCycle());
        subscription.reschedule(request.nextPaymentDate());
        subscription.setFreeTrial(request.freeTrial());
    }
}
