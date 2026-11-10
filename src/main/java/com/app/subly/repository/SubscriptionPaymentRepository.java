package com.app.subly.repository;

import com.app.subly.model.subscription.SubscriptionPayment;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    default List<SubscriptionPayment> findHistoryOf(String username) {
        return findByUserUsernameOrderByPaidOnDescIdDesc(username);
    }

    default List<SubscriptionPayment> findPaidSince(String username, LocalDate from) {
        return findByUserUsernameAndPaidOnGreaterThanEqual(username, from);
    }

    @EntityGraph(attributePaths = "subscription.provider")
    List<SubscriptionPayment> findByUserUsernameOrderByPaidOnDescIdDesc(String username);

    @EntityGraph(attributePaths = "subscription")
    List<SubscriptionPayment> findByUserUsernameAndPaidOnGreaterThanEqual(String username, LocalDate from);
}
