package com.app.subly.testdata;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.Currency;
import com.app.subly.service.share.MemberShare;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestMemberShares {

    public static MemberShare share(TestSubscriptions.Builder subscription) {
        Subscription built = subscription.build();
        return share(built, built.getPrice());
    }

    public static MemberShare share(TestSubscriptions.Builder subscription, String amount) {
        return share(subscription.build(), new BigDecimal(amount));
    }

    public static MemberShare cancelledShare(String amount, Currency currency, LocalDate endDate) {
        return share(
                aSubscription()
                        .withCurrency(currency)
                        .withNextPaymentDate(endDate)
                        .cancelled(),
                amount);
    }

    private static MemberShare share(Subscription subscription, BigDecimal amount) {
        return new MemberShare(SubscriptionMembership.owner(TestUsers.user(ALICE), subscription), amount);
    }
}
