package com.app.subly.model.invitation;

import com.app.subly.model.subscription.Subscription;
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
@Table(name = "subscription_pending")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingInvitation {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant invitedAt;

    public PendingInvitation(User user, Subscription subscription, User invitedBy) {
        this.user = user;
        this.subscription = subscription;
        this.invitedBy = invitedBy;
    }
}
