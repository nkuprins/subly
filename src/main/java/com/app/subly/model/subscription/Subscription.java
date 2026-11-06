package com.app.subly.model.subscription;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

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

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private LocalDate nextPaymentDate;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private LocalDate billingAnchorDate;

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

    public Subscription(
            ServiceProvider service,
            BigDecimal price,
            BillingCycle billingCycle,
            LocalDate nextPaymentDate,
            boolean freeTrial) {
        this.service = service;
        this.price = price;
        this.billingCycle = billingCycle;
        billingAnchorDate = nextPaymentDate;
        this.nextPaymentDate = nextPaymentDate;
        this.freeTrial = freeTrial;
    }

    public void reschedule(LocalDate nextPaymentDate) {
        billingAnchorDate = nextPaymentDate;
        this.nextPaymentDate = nextPaymentDate;
    }

    public void advancePaymentDate(LocalDate today) {
        ChronoUnit unit = switch (billingCycle) {
            case MONTHLY -> ChronoUnit.MONTHS;
            case YEARLY -> ChronoUnit.YEARS;
        };
        long cycles = Math.max(0, unit.between(billingAnchorDate, today));
        LocalDate date = billingAnchorDate.plus(cycles, unit);
        nextPaymentDate = date.isBefore(today) ? billingAnchorDate.plus(cycles + 1, unit) : date;
    }
}
