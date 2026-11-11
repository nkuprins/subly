package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;

import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.service.SubscriptionShares.Share;
import com.app.subly.testdata.TestSubscriptions;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;

final class TestShares {

    private TestShares() {}

    static Share share(TestSubscriptions.Builder subscription) {
        Subscription built = subscription.build();
        return new Share(SubscriptionMembership.owner(TestUsers.user(ALICE), built), built.getPrice());
    }

    static Share share(TestSubscriptions.Builder subscription, String amount) {
        return new Share(
                SubscriptionMembership.owner(TestUsers.user(ALICE), subscription.build()), new BigDecimal(amount));
    }

    static Share cancelledShare(String amount, Currency currency, LocalDate endDate) {
        return share(
                aSubscription()
                        .withCurrency(currency)
                        .withNextPaymentDate(endDate)
                        .cancelled(),
                amount);
    }
}
