package com.app.subly.service.access;

import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.MembershipRole;
import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.testdata.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionAccessTest {

    @Mock
    private SubscriptionMembershipRepository membershipRepository;

    @InjectMocks
    private SubscriptionAccess access;

    private final SubscriptionMembership owner =
            SubscriptionMembership.owner(TestUsers.user(ALICE), aSubscription().build());

    @Test
    void getMembership_shouldReturnTheUsersMembership() {
        when(membershipRepository.findBySubscriptionIdAndUserUsername(SUBSCRIPTION_ID, ALICE))
                .thenReturn(Optional.of(owner));

        assertThat(access.getMembership(ALICE, SUBSCRIPTION_ID)).isSameAs(owner);
    }

    @Test
    void getMembership_shouldRejectUserOutsideTheSubscription() {
        when(membershipRepository.findBySubscriptionIdAndUserUsername(SUBSCRIPTION_ID, BOB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> access.getMembership(BOB, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining(String.valueOf(SUBSCRIPTION_ID));
    }

    @Test
    void getOwnedSubscription_shouldReturnTheSubscriptionOfTheOwner() {
        stubOwnerMembership();

        assertThat(access.getOwnedSubscription(ALICE, SUBSCRIPTION_ID)).isSameAs(owner.getSubscription());
    }

    @Test
    void getOwnerMembership_shouldRejectNonOwner() {
        stubOwnerMembership();

        assertThatThrownBy(() -> access.getOwnerMembership(BOB, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class)
                .hasMessageContaining(String.valueOf(SUBSCRIPTION_ID));
    }

    @Test
    void getOwnerMembership_shouldRejectUnknownSubscription() {
        when(membershipRepository.findBySubscriptionIdAndRole(SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> access.getOwnerMembership(ALICE, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining(String.valueOf(SUBSCRIPTION_ID));
    }

    private void stubOwnerMembership() {
        when(membershipRepository.findBySubscriptionIdAndRole(SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.of(owner));
    }
}
