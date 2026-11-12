package com.app.subly.testdata;

import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestSubscriptions {

    public static final long SUBSCRIPTION_ID = 7L;
    public static final BigDecimal DEFAULT_PRICE = new BigDecimal("35.00");
    public static final LocalDate DEFAULT_NEXT_PAYMENT_DATE = TestClock.TODAY.plusDays(10);

    public static Builder aSubscription() {
        return new Builder();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {

        private Long id;
        private SubscriptionProvider provider = TestProviders.catalog(TestProviders.category());
        private BigDecimal price = DEFAULT_PRICE;
        private BillingCycle billingCycle = BillingCycle.MONTHLY;
        private LocalDate nextPaymentDate = DEFAULT_NEXT_PAYMENT_DATE;
        private boolean freeTrial;
        private Currency currency = Currency.EUR;
        private Integer maxParticipants;
        private boolean cancelled;

        public Builder withId(long id) {
            this.id = id;
            return this;
        }

        public Builder withProvider(SubscriptionProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder withPrice(String price) {
            this.price = new BigDecimal(price);
            return this;
        }

        public Builder withBillingCycle(BillingCycle billingCycle) {
            this.billingCycle = billingCycle;
            return this;
        }

        public Builder withNextPaymentDate(LocalDate nextPaymentDate) {
            this.nextPaymentDate = nextPaymentDate;
            return this;
        }

        public Builder withFreeTrial() {
            freeTrial = true;
            return this;
        }

        public Builder withCurrency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Builder withMaxParticipants(int maxParticipants) {
            this.maxParticipants = maxParticipants;
            return this;
        }

        public Builder cancelled() {
            cancelled = true;
            return this;
        }

        public Subscription build() {
            Subscription subscription =
                    new Subscription(provider, price, currency, billingCycle, nextPaymentDate, freeTrial);
            subscription.setId(id);
            if (maxParticipants != null) {
                subscription.makeSharable(maxParticipants);
            }
            if (cancelled) {
                subscription.cancel(TestClock.NOW);
            }
            return subscription;
        }
    }
}
