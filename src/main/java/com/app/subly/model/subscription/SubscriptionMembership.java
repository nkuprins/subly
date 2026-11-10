package com.app.subly.model.subscription;

import com.app.subly.model.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "subscription_membership")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionMembership {

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipRole role;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant joinedAt;

    public SubscriptionMembership(User user, Subscription subscription, MembershipRole role) {
        this.user = user;
        this.subscription = subscription;
        this.role = role;
    }

    public static SubscriptionMembership owner(User user, Subscription subscription) {
        return new SubscriptionMembership(user, subscription, MembershipRole.OWNER);
    }
}
