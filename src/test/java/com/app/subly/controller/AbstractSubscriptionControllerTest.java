package com.app.subly.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.config.SecurityConfig;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.ServiceType;
import com.app.subly.service.SubscriptionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(SubscriptionController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = AbstractSubscriptionControllerTest.USERNAME)
abstract class AbstractSubscriptionControllerTest {

    static final String USERNAME = "alice";
    static final String URL = "/api/subscriptions";
    static final long SUBSCRIPTION_ID = 7L;
    static final long SERVICE_ID = 1L;
    static final long CATEGORY_ID = 4L;
    static final String CUSTOM_NAME = "My gym";
    static final LocalDate TODAY = LocalDate.now();
    static final LocalDate NEXT_PAYMENT = TODAY.plusDays(10);
    private static final String VALIDATION_FAILED = "Invalid request content.";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SubscriptionService service;

    ResultActions send(BodyEndpoint endpoint, String body) throws Exception {
        return mockMvc.perform(endpoint.request().contentType(APPLICATION_JSON).content(body));
    }

    static ResultMatcher validationError() {
        return result -> {
            status().isBadRequest().match(result);
            jsonPath("$.detail").value(VALIDATION_FAILED).match(result);
        };
    }

    static SubscriptionResponse gymResponse() {
        return new SubscriptionResponse(
                SUBSCRIPTION_ID,
                CUSTOM_NAME,
                CATEGORY_ID,
                "Gym",
                ServiceType.CUSTOM,
                new BigDecimal("35.00"),
                Currency.EUR,
                BillingCycle.MONTHLY,
                NEXT_PAYMENT,
                false);
    }

    enum BodyEndpoint {
        CREATE(HttpMethod.POST, URL),
        CREATE_CUSTOM(HttpMethod.POST, URL + "/custom"),
        UPDATE(HttpMethod.PUT, URL + "/" + SUBSCRIPTION_ID),
        UPDATE_CUSTOM(HttpMethod.PUT, URL + "/" + SUBSCRIPTION_ID + "/custom");

        private final HttpMethod method;
        private final String path;

        BodyEndpoint(HttpMethod method, String path) {
            this.method = method;
            this.path = path;
        }

        MockHttpServletRequestBuilder request() {
            return MockMvcRequestBuilders.request(method, path);
        }

        String validBody() {
            return json(validFields());
        }

        String bodyWith(String field, Object value) {
            Map<String, Object> fields = validFields();
            fields.put(field, value);
            return json(fields);
        }

        String bodyWithout(String field) {
            Map<String, Object> fields = validFields();
            fields.remove(field);
            return json(fields);
        }

        private Map<String, Object> validFields() {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("price", new BigDecimal("35.00"));
            fields.put("billingCycle", "MONTHLY");
            fields.put("nextPaymentDate", NEXT_PAYMENT);
            switch (this) {
                case CREATE -> fields.put("serviceId", SERVICE_ID);
                case CREATE_CUSTOM, UPDATE_CUSTOM -> {
                    fields.put("name", CUSTOM_NAME);
                    fields.put("categoryId", CATEGORY_ID);
                }
                case UPDATE -> {}
            }
            return fields;
        }

        private static String json(Map<String, Object> fields) {
            return JSON.writeValueAsString(fields);
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
