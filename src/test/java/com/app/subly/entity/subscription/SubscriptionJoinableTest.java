package com.app.subly.entity.subscription;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.app.subly.testdata.TestSubscriptions;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SubscriptionJoinableTest {

    private static final int MAX_PARTICIPANTS = 3;

    @ParameterizedTest(name = "{0} -> closed: {3}")
    @MethodSource("subscriptions")
    void isClosedToNewMembers_shouldCloseFullCancelledOrNotSharableSubscriptions(
            String label, Subscription subscription, long memberCount, boolean closed) {
        assertThat(subscription.isClosedToNewMembers(memberCount)).isEqualTo(closed);
    }

    static Stream<Arguments> subscriptions() {
        return Stream.of(
                arguments("free seat", sharable().build(), MAX_PARTICIPANTS - 1, false),
                arguments("every seat taken", sharable().build(), MAX_PARTICIPANTS, true),
                arguments("cancelled", sharable().cancelled().build(), MAX_PARTICIPANTS - 1, true),
                arguments("not sharable", aSubscription().build(), 0, true));
    }

    @Test
    void makeSharable_shouldRejectFewerThanTwoParticipants() {
        Subscription subscription = aSubscription().build();

        assertThatThrownBy(() -> subscription.makeSharable(1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static TestSubscriptions.Builder sharable() {
        return aSubscription().withMaxParticipants(MAX_PARTICIPANTS);
    }
}
