package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.service.SubscriptionShares.Share;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionSharesTest {

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @InjectMocks
    SubscriptionShares subscriptionShares;

    @Test
    void load_shouldReturnEmptyWithoutLoadingMembersWhenUserHasNoSubscriptions() {
        stubAliceMemberships(List.of());

        assertThat(subscriptionShares.load(ALICE, SubscriptionStatus.ACTIVE)).isEmpty();
        verify(membershipRepository, never()).findBySubscriptionIdIn(any());
    }

    @Test
    void load_shouldSplitEachSubscriptionByItsOwnMembers() {
        Subscription shared = subscription(1L, "10.00");
        Subscription solo = subscription(2L, "6.00");
        SubscriptionMembership aliceInShared =
                new SubscriptionMembership(TestUsers.user(ALICE), shared, MembershipRole.MEMBER);
        SubscriptionMembership bobOwnsShared = SubscriptionMembership.owner(TestUsers.user(BOB), shared);
        SubscriptionMembership aliceOwnsSolo = SubscriptionMembership.owner(TestUsers.user(ALICE), solo);
        stubAliceMemberships(List.of(aliceInShared, aliceOwnsSolo));
        when(membershipRepository.findBySubscriptionIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(aliceInShared, bobOwnsShared, aliceOwnsSolo));

        assertThat(subscriptionShares.load(ALICE, SubscriptionStatus.ACTIVE))
                .extracting(Share::membership, Share::amount)
                .containsExactly(
                        tuple(aliceInShared, new BigDecimal("5.00")), tuple(aliceOwnsSolo, new BigDecimal("6.00")));
    }

    private void stubAliceMemberships(List<SubscriptionMembership> memberships) {
        when(membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        ALICE, SubscriptionStatus.ACTIVE))
                .thenReturn(memberships);
    }

    private static Subscription subscription(long id, String price) {
        return aSubscription().withId(id).withPrice(price).build();
    }
}
