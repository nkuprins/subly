package com.app.subly.services;

import com.app.subly.dto.subscription.CreateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.PaymentDetails;
import com.app.subly.dto.subscription.UpdateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.exceptions.subscription.CatalogProviderReadOnlyException;
import com.app.subly.exceptions.subscription.CategoryNotFoundException;
import com.app.subly.exceptions.subscription.CatalogProviderNotFoundException;
import com.app.subly.exceptions.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exceptions.subscription.SubscriptionNotFoundException;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.SubscriptionProvider;
import com.app.subly.models.subscription.ProviderType;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.models.subscription.SubscriptionMembership;
import com.app.subly.models.User;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMembershipRepository membershipRepository;
    private final SubscriptionProviderRepository providerRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubscriptionResponse create(String username, CreateSubscriptionRequest request) {
        User owner = findUser(username);
        SubscriptionProvider provider = findCatalogProvider(request.providerId());
        return subscribe(owner, provider, request);
    }

    @Transactional
    public SubscriptionResponse createCustom(String username, CreateCustomSubscriptionRequest request) {
        User owner = findUser(username);
        Category category = findCategory(request.categoryId());
        SubscriptionProvider provider = providerRepository.save(
                SubscriptionProvider.custom(request.name().strip(), category, owner));
        return subscribe(owner, provider, request);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> findOwnedBy(String username) {
        return membershipRepository
                .findByUserUsernameAndRoleOrderBySubscriptionNextPaymentDate(username, MembershipRole.OWNER)
                .stream()
                .map(membership -> SubscriptionResponse.from(membership.getSubscription()))
                .toList();
    }

    @Transactional
    public SubscriptionResponse update(String username, long subscriptionId, UpdateSubscriptionRequest request) {
        Subscription subscription = findOwnedSubscription(username, subscriptionId);
        applyPaymentDetails(subscription, request);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse updateCustom(String username, long subscriptionId,
                                             UpdateCustomSubscriptionRequest request) {
        Subscription subscription = findOwnedSubscription(username, subscriptionId);
        SubscriptionProvider provider = subscription.getProvider();
        if (provider.getProviderType() != ProviderType.CUSTOM) {
            throw new CatalogProviderReadOnlyException(provider.getName());
        }
        Category category = findCategory(request.categoryId());
        provider.setName(request.name().strip());
        provider.setCategory(category);
        applyPaymentDetails(subscription, request);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public void delete(String username, long subscriptionId) {
        Subscription subscription = findOwnedSubscription(username, subscriptionId);
        SubscriptionProvider provider = subscription.getProvider();

        //  Decide what happens to payments when they are implemented.
        subscriptionRepository.delete(subscription);
        if (provider.getProviderType() == ProviderType.CUSTOM) {
            providerRepository.delete(provider);
        }
    }

    private SubscriptionResponse subscribe(User owner, SubscriptionProvider provider, PaymentDetails request) {
        // Placeholder: the form has no start date yet, so the subscription starts on the day it is added.
        LocalDate startedAt = LocalDate.now();
        Subscription subscription = new Subscription(
                provider,
                startedAt,
                request.price(),
                request.billingCycle(),
                request.nextPaymentDate(),
                request.freeTrial());
        subscriptionRepository.save(subscription);

        membershipRepository.save(SubscriptionMembership.owner(owner, subscription));
        return SubscriptionResponse.from(subscription);
    }

    private Subscription findOwnedSubscription(String username, long subscriptionId) {
        Subscription subscription = subscriptionRepository.findWithProviderById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        boolean isOwner = membershipRepository.existsBySubscriptionIdAndUserUsernameAndRole(
                subscriptionId, username, MembershipRole.OWNER);
        if (!isOwner) {
            throw new SubscriptionAccessDeniedException(subscriptionId);
        }
        return subscription;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User " + username + " does not exist"));
    }

    private SubscriptionProvider findCatalogProvider(long providerId) {
        return providerRepository.findByIdAndProviderType(providerId, ProviderType.CATALOG)
                .orElseThrow(() -> new CatalogProviderNotFoundException(providerId));
    }

    private Category findCategory(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private static void applyPaymentDetails(Subscription subscription, PaymentDetails request) {
        subscription.setPrice(request.price());
        subscription.setBillingCycle(request.billingCycle());
        subscription.setNextPaymentDate(request.nextPaymentDate());
        subscription.setFreeTrial(request.freeTrial());
    }
}
