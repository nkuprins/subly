package com.app.subly.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentValidationControllerTest extends AbstractSubscriptionControllerTest {

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidPaymentDetails")
    void rejectsInvalidPaymentDetails(Endpoint endpoint, String description, String body) throws Exception {
        send(endpoint, body)
                .andExpect(validationError());

        then(service).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("validPaymentDetails")
    void acceptsBoundaryPaymentDetails(Endpoint endpoint, String description, String body) throws Exception {
        send(endpoint, body)
                .andExpect(status().is2xxSuccessful());
    }

    static Stream<Arguments> invalidPaymentDetails() {
        return Arrays.stream(Endpoint.values()).flatMap(endpoint -> Stream.of(
                arguments(endpoint, "price 0", endpoint.bodyWith("price", 0)),
                arguments(endpoint, "negative price", endpoint.bodyWith("price", -1)),
                arguments(endpoint, "price with 3 decimals", endpoint.bodyWith("price", new BigDecimal("9.999"))),
                arguments(endpoint, "price with 9 integer digits",
                        endpoint.bodyWith("price", new BigDecimal("123456789.00"))),
                arguments(endpoint, "payment date in the past",
                        endpoint.bodyWith("nextPaymentDate", TODAY.minusDays(1))),
                arguments(endpoint, "missing price", endpoint.bodyWithout("price")),
                arguments(endpoint, "missing billing cycle", endpoint.bodyWithout("billingCycle")),
                arguments(endpoint, "missing payment date", endpoint.bodyWithout("nextPaymentDate"))));
    }

    static Stream<Arguments> validPaymentDetails() {
        return Arrays.stream(Endpoint.values()).flatMap(endpoint -> Stream.of(
                arguments(endpoint, "price 0.01", endpoint.bodyWith("price", new BigDecimal("0.01"))),
                arguments(endpoint, "price 99999999.99", endpoint.bodyWith("price", new BigDecimal("99999999.99")))));
    }
}
