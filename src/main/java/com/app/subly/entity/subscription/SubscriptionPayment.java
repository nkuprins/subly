package com.app.subly.entity.subscription;

import com.app.subly.entity.user.User;
import com.app.subly.util.MoneyMath;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "subscription_payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPayment {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(
            nullable = false,
            precision = MoneyMath.INTEGER_DIGITS + MoneyMath.FRACTION_DIGITS,
            scale = MoneyMath.FRACTION_DIGITS)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paidOn;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    public SubscriptionPayment(Subscription subscription, User user, BigDecimal amount, LocalDate paidOn) {
        this.subscription = subscription;
        this.user = user;
        this.amount = amount;
        this.paidOn = paidOn;
    }
}
