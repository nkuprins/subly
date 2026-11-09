package com.app.subly.testutil;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.HttpMethod;

public record ValidationCases(String label, HttpMethod method, String path, JsonBody validBody) {

    public Arguments with(String field, Object value, String reason) {
        return arguments(
                label + ": " + field + " " + reason, request(method, path), validBody.with(field, value), field);
    }

    public Arguments without(String field) {
        return arguments(label + ": missing " + field, request(method, path), validBody.without(field), field);
    }
}
