package com.app.subly.controller.subscription;

import static com.app.subly.testdata.TestProviders.CATEGORY_NAME;
import static com.app.subly.testdata.TestProviders.CUSTOM_PROVIDER_NAME;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestUsers.ALICE;

import com.app.subly.controller.AbstractWebMvcTest;
import com.app.subly.controller.SubscriptionController;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.service.SubscriptionService;
import com.app.subly.testutil.JsonBody;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(SubscriptionController.class)
@WithMockUser(username = ALICE)
abstract class AbstractSubscriptionControllerTest extends AbstractWebMvcTest {

    static final String URL = "/api/subscriptions";
    static final long SUBSCRIPTION_ID = 7L;
    static final String ITEM_URL = URL + "/" + SUBSCRIPTION_ID;
    static final long PROVIDER_ID = 1L;
    static final long CATEGORY_ID = 4L;
    static final JsonBody VALID_UPDATE_BODY = JsonBody.empty()
            .with("price", DEFAULT_PRICE)
            .with("billingCycle", "MONTHLY")
            .with("nextPaymentDate", DEFAULT_NEXT_PAYMENT_DATE);
    static final JsonBody VALID_CREATE_BODY = VALID_UPDATE_BODY.with("providerId", PROVIDER_ID);

    @MockitoBean
    SubscriptionService service;

    static SubscriptionResponse subscriptionResponse() {
        return new SubscriptionResponse(
                SUBSCRIPTION_ID,
                PROVIDER_ID,
                CUSTOM_PROVIDER_NAME,
                CATEGORY_ID,
                CATEGORY_NAME,
                ProviderType.CUSTOM,
                DEFAULT_PRICE,
                Currency.EUR,
                BillingCycle.MONTHLY,
                DEFAULT_NEXT_PAYMENT_DATE,
                false,
                1,
                SubscriptionStatus.ACTIVE);
    }
}
