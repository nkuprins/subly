package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.SubscriptionMembership;
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

    static final long SUBSCRIPTION_ID = 7L;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @InjectMocks
    SubscriptionAccess access;

    final SubscriptionMembership ownerRow =
            SubscriptionMembership.owner(TestUsers.user(ALICE), aSubscription().build());

    @Test
    void getOwnedSubscription_shouldReturnTheSubscriptionOfTheOwner() {
        stubOwnerRow();

        assertThat(access.getOwnedSubscription(ALICE, SUBSCRIPTION_ID)).isSameAs(ownerRow.getSubscription());
    }

    @Test
    void getOwnerMembership_shouldRejectNonOwner() {
        stubOwnerRow();

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

    @Test
    void getOwnerMembershipWithSubscriptionDetails_shouldReturnTheOwnerRow() {
        stubOwnerRowWithSubscriptionDetails();

        assertThat(access.getOwnerMembershipWithSubscriptionDetails(ALICE, SUBSCRIPTION_ID))
                .isSameAs(ownerRow);
    }

    @Test
    void getOwnerMembershipWithSubscriptionDetails_shouldRejectNonOwner() {
        stubOwnerRowWithSubscriptionDetails();

        assertThatThrownBy(() -> access.getOwnerMembershipWithSubscriptionDetails(BOB, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class)
                .hasMessageContaining(String.valueOf(SUBSCRIPTION_ID));
    }

    @Test
    void getOwnerMembershipWithSubscriptionDetails_shouldRejectUnknownSubscription() {
        when(membershipRepository.findWithSubscriptionDetailsBySubscriptionIdAndRole(
                        SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> access.getOwnerMembershipWithSubscriptionDetails(ALICE, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining(String.valueOf(SUBSCRIPTION_ID));
    }

    private void stubOwnerRow() {
        when(membershipRepository.findBySubscriptionIdAndRole(SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.of(ownerRow));
    }

    private void stubOwnerRowWithSubscriptionDetails() {
        when(membershipRepository.findWithSubscriptionDetailsBySubscriptionIdAndRole(
                        SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.of(ownerRow));
    }
}
