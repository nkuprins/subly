package com.app.subly.repository;

import com.app.subly.model.subscription.SubscriptionPayment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    default List<SubscriptionPayment> findHistoryOf(String username) {
        return findByUserUsernameOrderByPaidOnDescIdDesc(username);
    }

    @EntityGraph(attributePaths = "subscription.provider")
    List<SubscriptionPayment> findByUserUsernameOrderByPaidOnDescIdDesc(String username);
}
