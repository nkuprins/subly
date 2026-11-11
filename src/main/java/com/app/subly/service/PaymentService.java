package com.app.subly.service;

import com.app.subly.dto.payment.PaymentResponse;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionPaymentRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public List<PaymentResponse> findHistory(String username) {
        return paymentRepository.findHistoryOf(username).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public void advancePaymentDates(ZoneId zone, LocalDate today) {
        for (SubscriptionMembership owner :
                membershipRepository.findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                        MembershipRole.OWNER, SubscriptionStatus.ACTIVE, zone, today)) {
            Subscription subscription = owner.getSubscription();
            List<SubscriptionMembership> members =
                    membershipRepository.findBySubscriptionIdOrderByIdAsc(subscription.getId());
            paymentRepository.saveAll(subscription.paymentsDue(members));
            subscription.moveToNextCycle();
        }
    }
}
