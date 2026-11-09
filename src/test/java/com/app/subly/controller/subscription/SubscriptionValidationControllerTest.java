package com.app.subly.controller.subscription;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.testutil.JsonBody;
import com.app.subly.testutil.ValidationCases;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class SubscriptionValidationControllerTest extends AbstractSubscriptionControllerTest {

    static final ValidationCases CREATE = new ValidationCases("create", HttpMethod.POST, URL, VALID_CREATE_BODY);
    static final ValidationCases UPDATE = new ValidationCases("update", HttpMethod.PUT, ITEM_URL, VALID_UPDATE_BODY);

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void createAndUpdate_shouldRejectInvalidBody(
            String description, MockHttpServletRequestBuilder request, JsonBody body, String field) throws Exception {
        sendJson(request, body)
                .andExpect(validationError())
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validPaymentDetails")
    void createAndUpdate_shouldAcceptBoundaryPaymentDetails(
            String description, MockHttpServletRequestBuilder request, JsonBody body) throws Exception {
        sendJson(request, body).andExpect(status().is2xxSuccessful());
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                CREATE.without("providerId"),
                CREATE.with("price", 0, "zero"),
                CREATE.with("price", -1, "negative"),
                CREATE.with("price", new BigDecimal("9.999"), "with 3 decimals"),
                CREATE.with("price", new BigDecimal("123456789.00"), "with 9 integer digits"),
                CREATE.without("price"),
                CREATE.without("billingCycle"),
                CREATE.without("nextPaymentDate"),
                UPDATE.with("price", 0, "zero"),
                UPDATE.with("price", -1, "negative"),
                UPDATE.with("price", new BigDecimal("9.999"), "with 3 decimals"),
                UPDATE.with("price", new BigDecimal("123456789.00"), "with 9 integer digits"),
                UPDATE.without("price"),
                UPDATE.without("billingCycle"),
                UPDATE.without("nextPaymentDate"));
    }

    static Stream<Arguments> validPaymentDetails() {
        return Stream.of(
                arguments("create: price 0.01", post(URL), VALID_CREATE_BODY.with("price", new BigDecimal("0.01"))),
                arguments(
                        "create: price 99999999.99",
                        post(URL),
                        VALID_CREATE_BODY.with("price", new BigDecimal("99999999.99"))),
                arguments("update: price 0.01", put(ITEM_URL), VALID_UPDATE_BODY.with("price", new BigDecimal("0.01"))),
                arguments(
                        "update: price 99999999.99",
                        put(ITEM_URL),
                        VALID_UPDATE_BODY.with("price", new BigDecimal("99999999.99"))));
    }
}
