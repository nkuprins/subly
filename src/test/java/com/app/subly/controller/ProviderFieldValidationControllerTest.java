package com.app.subly.controller;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.then;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProviderFieldValidationControllerTest extends AbstractSubscriptionControllerTest {

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidProviderFields")
    void rejectsInvalidProviderFields(BodyEndpoint endpoint, String description, String body) throws Exception {
        send(endpoint, body).andExpect(validationError());

        then(service).shouldHaveNoInteractions();
    }

    static Stream<Arguments> invalidProviderFields() {
        Stream<Arguments> catalog = Stream.of(
                arguments(BodyEndpoint.CREATE, "missing provider id", BodyEndpoint.CREATE.bodyWithout("providerId")));
        Stream<Arguments> custom = Stream.of(BodyEndpoint.CREATE_CUSTOM, BodyEndpoint.UPDATE_CUSTOM)
                .flatMap(endpoint -> Stream.of(
                        arguments(endpoint, "blank name", endpoint.bodyWith("name", "   ")),
                        arguments(
                                endpoint,
                                "name longer than 200 characters",
                                endpoint.bodyWith("name", "x".repeat(201))),
                        arguments(endpoint, "missing name", endpoint.bodyWithout("name")),
                        arguments(endpoint, "missing category", endpoint.bodyWithout("categoryId"))));
        return Stream.concat(catalog, custom);
    }
}
