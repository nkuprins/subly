package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.admin.AdminStatisticsResponse;
import com.app.subly.dto.admin.PopularServiceResponse;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.ProviderType;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    AdminStatisticsService service;

    @Test
    void getStatistics_shouldCountActiveSubscriptionsAndRankCatalogServices() {
        SubscriptionProvider netflix = catalogProvider(5L, "Netflix");
        SubscriptionProvider appleTv = catalogProvider(6L, "Apple TV");
        when(userRepository.count()).thenReturn(4L);
        when(subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)).thenReturn(3L);
        when(subscriptionRepository.findByStatusAndProviderProviderType(
                        SubscriptionStatus.ACTIVE, ProviderType.CATALOG))
                .thenReturn(List.of(
                        aSubscription().withProvider(netflix).build(),
                        aSubscription().withProvider(appleTv).build(),
                        aSubscription().withProvider(netflix).build()));

        AdminStatisticsResponse result = service.getStatistics();

        assertThat(result.totalUsers()).isEqualTo(4);
        assertThat(result.activeSubscriptions()).isEqualTo(3);
        assertThat(result.popularServices())
                .containsExactly(new PopularServiceResponse("Netflix", 2L), new PopularServiceResponse("Apple TV", 1L));
    }

    @Test
    void getStatistics_shouldAverageEachMemberShareAcrossAllUsers() {
        Subscription subscription =
                aSubscription().withId(1L).withPrice("30.00").build();
        SubscriptionMembership owner = SubscriptionMembership.owner(TestUsers.user(ALICE), subscription);
        SubscriptionMembership bobMember = SubscriptionMembership.member(TestUsers.user(BOB), subscription);
        when(userRepository.count()).thenReturn(3L);
        when(membershipRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(owner, bobMember));

        AdminStatisticsResponse result = service.getStatistics();

        // One shared plan costs EUR 30/month, two members pay EUR 15 each,
        // and the third registered user spends EUR 0: (15 + 15 + 0) / 3 = 10.
        assertThat(result.averageMonthlySpendPerUser()).isEqualByComparingTo("10.00");
    }

    private static SubscriptionProvider catalogProvider(long id, String name) {
        SubscriptionProvider provider = SubscriptionProvider.catalog(name, TestProviders.category());
        provider.setId(id);
        return provider;
    }
}
