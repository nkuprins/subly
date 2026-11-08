package com.app.subly.model.subscription;

import com.app.subly.model.provider.SubscriptionProvider;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    public static final int PRICE_INTEGER_DIGITS = 8;

    public static final int PRICE_FRACTION_DIGITS = 2;

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private SubscriptionProvider provider;

    @Setter
    @Column(nullable = false, precision = PRICE_INTEGER_DIGITS + PRICE_FRACTION_DIGITS, scale = PRICE_FRACTION_DIGITS)
    private BigDecimal price;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;

    @Column(nullable = false)
    private LocalDate nextPaymentDate;

    @Column(nullable = false)
    private LocalDate billingAnchorDate;

    @Setter
    @Column(nullable = false)
    private boolean freeTrial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    private Instant cancelledAt;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    // Placeholder: always EUR until the user can choose a currency.
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency = Currency.EUR;

    // Placeholders: not sharable, one participant, until shared plans exist.
    @Column(name = "is_sharable", nullable = false)
    private boolean sharable = false;

    @Column(nullable = false)
    private int maxParticipants = 1;

    public Subscription(
            SubscriptionProvider provider,
            BigDecimal price,
            BillingCycle billingCycle,
            LocalDate nextPaymentDate,
            boolean freeTrial) {
        this.provider = provider;
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

    public void makeSharable(int maxParticipants) {
        if (maxParticipants < 1) {
            throw new IllegalArgumentException("maxParticipants must be at least 1, got " + maxParticipants);
        }
        sharable = true;
        this.maxParticipants = maxParticipants;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public void cancel(Instant now) {
        status = SubscriptionStatus.CANCELLED;
        cancelledAt = now;
    }

    public void reactivate(LocalDate today) {
        advancePaymentDate(today);
        status = SubscriptionStatus.ACTIVE;
        cancelledAt = null;
    }

    public void advancePaymentDate(LocalDate today) {
        ChronoUnit unit = billingCycle.getUnit();
        long cycles = Math.max(0, unit.between(billingAnchorDate, today));
        LocalDate date = billingAnchorDate.plus(cycles, unit);
        nextPaymentDate = date.isBefore(today) ? billingAnchorDate.plus(cycles + 1, unit) : date;
    }
}
