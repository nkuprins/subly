package com.app.subly.models.subscription;

import com.app.subly.models.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_pending")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Subscription subscription;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime invitedAt;

    public PendingInvitation(User user, Subscription subscription) {
        this.user = user;
        this.subscription = subscription;
    }
}
