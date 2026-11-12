package com.app.subly.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(SecurityConfig.class)
public abstract class AbstractWebMvcTest {

    private static final String VALIDATION_FAILED = "Invalid request content.";

    @Autowired
    protected MockMvc mockMvc;

    protected ResultActions sendJson(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mockMvc.perform(request.contentType(APPLICATION_JSON).content(json));
    }

    protected static ResultMatcher validationError() {
        return result -> {
            status().isBadRequest().match(result);
            jsonPath("$.detail").value(VALIDATION_FAILED).match(result);
            jsonPath("$.errors").isNotEmpty().match(result);
        };
    }

    protected static ResultMatcher problem(HttpStatus status, Exception exception) {
        return result -> {
            status().is(status.value()).match(result);
            jsonPath("$.detail").value(exception.getMessage()).match(result);
        };
    }
}
