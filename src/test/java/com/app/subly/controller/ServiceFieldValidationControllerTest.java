package com.app.subly.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.BDDMockito.then;

class ServiceFieldValidationControllerTest extends AbstractSubscriptionControllerTest {

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidServiceFields")
    void rejectsInvalidServiceFields(Endpoint endpoint, String description, String body) throws Exception {
        send(endpoint, body)
                .andExpect(validationError());

        then(service).shouldHaveNoInteractions();
    }

    static Stream<Arguments> invalidServiceFields() {
        Stream<Arguments> catalog = Stream.of(
                arguments(Endpoint.CREATE, "missing service id", Endpoint.CREATE.bodyWithout("serviceId")));
        Stream<Arguments> custom = Stream.of(Endpoint.CREATE_CUSTOM, Endpoint.UPDATE_CUSTOM)
                .flatMap(endpoint -> Stream.of(
                        arguments(endpoint, "blank name", endpoint.bodyWith("name", "   ")),
                        arguments(endpoint, "name longer than 200 characters",
                                endpoint.bodyWith("name", "x".repeat(201))),
                        arguments(endpoint, "missing name", endpoint.bodyWithout("name")),
                        arguments(endpoint, "missing category", endpoint.bodyWithout("categoryId"))));
        return Stream.concat(catalog, custom);
    }
}
