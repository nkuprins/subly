package com.app.subly.entity.reminder;

import com.app.subly.entity.subscription.SubscriptionMembership;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SubscriptionMembership membership;

    private LocalDate firstSentAt;
    private LocalDate secondSentAt;

    @Column(nullable = false)
    private boolean isActive;

    public SubscriptionReminder(SubscriptionMembership membership, boolean isActive) {
        this.membership = membership;
        this.isActive = isActive;
    }
}
