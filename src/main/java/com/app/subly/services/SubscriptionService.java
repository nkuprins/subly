package com.app.subly.services;

import com.app.subly.dto.subscription.CreateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.PaymentDetails;
import com.app.subly.dto.subscription.UpdateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.exceptions.subscription.CatalogServiceReadOnlyException;
import com.app.subly.exceptions.subscription.CategoryNotFoundException;
import com.app.subly.exceptions.subscription.CatalogServiceNotFoundException;
import com.app.subly.exceptions.subscription.PaymentDateInPastException;
import com.app.subly.exceptions.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exceptions.subscription.SubscriptionNotFoundException;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.ServiceProvider;
import com.app.subly.models.subscription.ServiceType;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.models.subscription.SubscriptionMembership;
import com.app.subly.models.User;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.ServiceProviderRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMembershipRepository membershipRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public SubscriptionResponse create(String username, CreateSubscriptionRequest request) {
        User owner = findUser(username);
        requireNotInPast(owner, request);
        ServiceProvider service = findCatalogService(request.serviceId());
        return subscribe(owner, service, request);
    }

    @Transactional
    public SubscriptionResponse createCustom(String username, CreateCustomSubscriptionRequest request) {
        User owner = findUser(username);
        requireNotInPast(owner, request);
        Category category = findCategory(request.categoryId());
        ServiceProvider service = serviceProviderRepository.save(
                ServiceProvider.custom(request.name().strip(), category, owner));
        return subscribe(owner, service, request);
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
        requireNotInPast(findUser(username), request);
        applyPaymentDetails(subscription, request);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse updateCustom(String username, long subscriptionId,
                                             UpdateCustomSubscriptionRequest request) {
        Subscription subscription = findOwnedSubscription(username, subscriptionId);
        ServiceProvider service = subscription.getService();
        if (service.getServiceType() != ServiceType.CUSTOM) {
            throw new CatalogServiceReadOnlyException(service.getName());
        }
        requireNotInPast(findUser(username), request);
        Category category = findCategory(request.categoryId());
        service.setName(request.name().strip());
        service.setCategory(category);
        applyPaymentDetails(subscription, request);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public void delete(String username, long subscriptionId) {
        Subscription subscription = findOwnedSubscription(username, subscriptionId);
        ServiceProvider service = subscription.getService();

        //  Decide what happens to payments when they are implemented.
        subscriptionRepository.delete(subscription);
        if (service.getServiceType() == ServiceType.CUSTOM) {
            serviceProviderRepository.delete(service);
        }
    }

    private SubscriptionResponse subscribe(User owner, ServiceProvider service, PaymentDetails request) {
        Subscription subscription = new Subscription(
                service,
                request.price(),
                request.billingCycle(),
                request.nextPaymentDate(),
                request.freeTrial());
        subscriptionRepository.save(subscription);

        membershipRepository.save(SubscriptionMembership.owner(owner, subscription));
        return SubscriptionResponse.from(subscription);
    }

    private Subscription findOwnedSubscription(String username, long subscriptionId) {
        Subscription subscription = subscriptionRepository.findWithServiceById(subscriptionId)
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

    private ServiceProvider findCatalogService(long serviceId) {
        return serviceProviderRepository.findByIdAndServiceType(serviceId, ServiceType.CATALOG)
                .orElseThrow(() -> new CatalogServiceNotFoundException(serviceId));
    }

    private Category findCategory(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
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
