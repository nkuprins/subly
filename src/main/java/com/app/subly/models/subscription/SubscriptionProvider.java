package com.app.subly.models.subscription;

import com.app.subly.models.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_providers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionProvider {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String logoPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType providerType;

    private SubscriptionProvider(String name, Category category, User createdBy, ProviderType providerType) {
        this.name = name;
        this.category = category;
        this.createdBy = createdBy;
        this.providerType = providerType;
    }

    public static SubscriptionProvider catalog(String name, Category category) {
        return new SubscriptionProvider(name, category, null, ProviderType.CATALOG);
    }

    public static SubscriptionProvider custom(String name, Category category, User creator) {
        return new SubscriptionProvider(name, category, creator, ProviderType.CUSTOM);
    }
}
