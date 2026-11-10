package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.admin.AdminStatisticsResponse;
import com.app.subly.dto.admin.PopularServiceResponse;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    AdminStatisticsService service;

    @BeforeEach
    void setUp() {
        service = new AdminStatisticsService(userRepository, subscriptionRepository, membershipRepository);
    }

    @Test
    void getStatistics_shouldCountActiveSubscriptionsAndRankCatalogServices() {
        when(userRepository.count()).thenReturn(3L);
        when(subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)).thenReturn(3L);

        SubscriptionProvider netflix = SubscriptionProvider.catalog("Netflix", TestProviders.category());
        netflix.setId(5L);

        SubscriptionProvider spotify = SubscriptionProvider.catalog("Spotify", TestProviders.category());
        spotify.setId(6L);

        Subscription netflix1 = aSubscription().withId(1L).withProvider(netflix).build();

        Subscription netflix2 = aSubscription().withId(2L).withProvider(netflix).build();

        Subscription spotify1 = aSubscription().withId(3L).withProvider(spotify).build();

        when(subscriptionRepository.findByStatusAndProviderProviderType(
                        SubscriptionStatus.ACTIVE, ProviderType.CATALOG))
                .thenReturn(List.of(netflix1, netflix2, spotify1));

        AdminStatisticsResponse result = service.getStatistics();

        assertThat(result.totalUsers()).isEqualTo(3);
        assertThat(result.activeSubscriptions()).isEqualTo(3);
        assertThat(result.popularServices())
                .containsExactly(
                        new PopularServiceResponse(5L, "Netflix", 2L), new PopularServiceResponse(6L, "Spotify", 1L));
    }

    @Test
    void getStatistics_shouldAverageEachMemberShareAcrossAllUsers() {
        when(userRepository.count()).thenReturn(3L);
        Subscription subscription =
                aSubscription().withId(1L).withPrice("30.00").build();
        SubscriptionMembership owner =
                new SubscriptionMembership(TestUsers.userWithId(ALICE, 1L), subscription, MembershipRole.OWNER);
        SubscriptionMembership member =
                new SubscriptionMembership(TestUsers.userWithId(BOB, 2L), subscription, MembershipRole.MEMBER);
        when(membershipRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(owner, member));

        AdminStatisticsResponse result = service.getStatistics();

        // One shared plan costs EUR 30/month, two members pay EUR 15 each,
        // and the third registered user spends EUR 0: (15 + 15 + 0) / 3 = 10.
        assertThat(result.averageMonthlySpendPerUser()).isEqualByComparingTo("10.00");
    }
}
