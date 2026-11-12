package com.app.subly.entity.subscription;

import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.util.MoneyMath;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private SubscriptionProvider provider;

    @Column(
            nullable = false,
            precision = MoneyMath.INTEGER_DIGITS + MoneyMath.FRACTION_DIGITS,
            scale = MoneyMath.FRACTION_DIGITS)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;

    @Column(nullable = false)
    private LocalDate nextPaymentDate;

    @Column(nullable = false)
    private LocalDate billingAnchorDate;

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

    public void changePaymentDetails(
            BigDecimal price, BillingCycle billingCycle, LocalDate nextPaymentDate, boolean freeTrial) {
        this.price = price;
        this.billingCycle = billingCycle;
        reschedule(nextPaymentDate);
        this.freeTrial = freeTrial;
    }

    public void makeSharable(int maxParticipants) {
        if (maxParticipants < 2) {
            throw new IllegalArgumentException("maxParticipants must be at least 2, got " + maxParticipants);
        }
        this.maxParticipants = maxParticipants;
    }

    public boolean isSharable() {
        return maxParticipants > 1;
    }

    public boolean isFull(long memberCount) {
        return memberCount >= maxParticipants;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isClosedToNewMembers(long memberCount) {
        return !isActive() || !isSharable() || isFull(memberCount);
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

    public void moveToNextCycle() {
        advancePaymentDate(nextPaymentDate.plusDays(1));
    }

    public List<SubscriptionPayment> paymentsDue(List<SubscriptionMembership> members) {
        List<SubscriptionPayment> payments = new ArrayList<>();
        for (SubscriptionMembership member : members) {
            BigDecimal amount = shareFor(member.getRole(), members.size());
            if (amount.signum() > 0) {
                payments.add(new SubscriptionPayment(this, member.getUser(), amount, nextPaymentDate));
            }
        }
        return payments;
    }

    public long skippedPaymentsUntil(LocalDate today) {
        if (nextPaymentDate.isAfter(today)) {
            return 0;
        }
        return billingCycle.getUnit().between(nextPaymentDate, today) + 1;
    }

    public BigDecimal shareFor(MembershipRole role, long memberCount) {
        if (memberCount <= 0) {
            throw new IllegalStateException("Subscription " + id + " has no members");
        }

        BigDecimal baseShare =
                price.divide(BigDecimal.valueOf(memberCount), MoneyMath.FRACTION_DIGITS, RoundingMode.DOWN);

        BigDecimal leftover = price.subtract(baseShare.multiply(BigDecimal.valueOf(memberCount)));
        return role == MembershipRole.OWNER ? baseShare.add(leftover) : baseShare;
    }
}
