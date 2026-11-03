package com.app.subly.models.subscription;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceProvider service;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;

    @Column(nullable = false)
    private LocalDate nextPaymentDate;

    @Column(nullable = false)
    private LocalDate subscriptionStartedAt;

    @Column(nullable = false)
    private boolean freeTrial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // Placeholder: always EUR until the user can choose a currency.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency = Currency.EUR;

    // Placeholders: not sharable, one participant, until shared plans exist.
    @Column(name = "is_sharable", nullable = false)
    private boolean sharable = false;

    @Column(nullable = false)
    private int maxParticipants = 1;

    public Subscription(ServiceProvider service, LocalDate subscriptionStartedAt, BigDecimal price,
                        BillingCycle billingCycle, LocalDate nextPaymentDate, boolean freeTrial) {
        this.service = service;
        this.subscriptionStartedAt = subscriptionStartedAt;
        this.price = price;
        this.billingCycle = billingCycle;
        this.nextPaymentDate = nextPaymentDate;
        this.freeTrial = freeTrial;
    }
}
