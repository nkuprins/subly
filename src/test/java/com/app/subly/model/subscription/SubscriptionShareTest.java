package com.app.subly.model.subscription;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubscriptionShareTest {

    @ParameterizedTest(name = "{0} split {1} ways, {2} -> {3}")
    @CsvSource({
        "10.00, 1, OWNER,  10.00", // alone: full price
        "10.00, 2, MEMBER, 5.00", // even split
        "10.00, 3, MEMBER, 3.33", // member: rounded down
        "10.00, 3, OWNER,  3.34", // owner: takes the leftover cent
    })
    void shareFor_shouldRoundMembersDownAndGiveTheOwnerTheRest(
            String price, long memberCount, MembershipRole role, String expected) {
        Subscription subscription = aSubscription().withPrice(price).build();

        assertThat(subscription.shareFor(role, memberCount)).isEqualByComparingTo(expected);
    }

    @Test
    void shareFor_shouldRejectZeroMembers() {
        Subscription subscription = aSubscription().build();

        assertThatThrownBy(() -> subscription.shareFor(MembershipRole.OWNER, 0))
                .isInstanceOf(IllegalStateException.class);
    }
}
