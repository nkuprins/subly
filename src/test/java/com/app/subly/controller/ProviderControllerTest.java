package com.app.subly.controller;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.config.SecurityConfig;
import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.exception.category.InvalidCategoryException;
import com.app.subly.exception.subscription.ProviderNotFoundException;
import com.app.subly.model.subscription.ProviderType;
import com.app.subly.service.ProviderService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(ProviderController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = ProviderControllerTest.USERNAME)
class ProviderControllerTest {

    static final String USERNAME = "alice";
    static final String URL = "/api/providers";
    static final long PROVIDER_ID = 3L;
    static final long CATEGORY_ID = 4L;
    static final String NAME = "My gym";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProviderService service;

    @Test
    void createReturns201WithTheCustomProvider() throws Exception {
        given(service.create(USERNAME, new ProviderRequest(NAME, CATEGORY_ID, null)))
                .willReturn(gymResponse());

        mockMvc.perform(post(URL).contentType(APPLICATION_JSON).content(json(validFields())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROVIDER_ID))
                .andExpect(jsonPath("$.name").value(NAME))
                .andExpect(jsonPath("$.providerType").value("CUSTOM"));
    }

    @Test
    void createReturns400ForUnknownCategory() throws Exception {
        given(service.create(anyString(), any())).willThrow(new InvalidCategoryException(99L));

        mockMvc.perform(post(URL).contentType(APPLICATION_JSON).content(json(validFields())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Category 99 does not exist"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void createRejectsInvalidFields(String description, String body) throws Exception {
        mockMvc.perform(post(URL).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());

        then(service).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void updateRejectsInvalidFields(String description, String body) throws Exception {
        mockMvc.perform(put(URL + "/" + PROVIDER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void listReturnsProvidersVisibleToTheUser() throws Exception {
        given(service.findVisibleTo(USERNAME)).willReturn(List.of(gymResponse()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROVIDER_ID))
                .andExpect(jsonPath("$[0].categoryName").value("Gym"));
    }

    @Test
    void updateReturns200() throws Exception {
        given(service.updateCustomProvider(USERNAME, PROVIDER_ID, new ProviderRequest(NAME, CATEGORY_ID, null)))
                .willReturn(gymResponse());

        mockMvc.perform(put(URL + "/" + PROVIDER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(json(validFields())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROVIDER_ID));
    }

    @Test
    void updateReturns400ForProviderTheUserDoesNotOwn() throws Exception {
        given(service.updateCustomProvider(anyString(), anyLong(), any()))
                .willThrow(new ProviderNotFoundException(PROVIDER_ID));

        mockMvc.perform(put(URL + "/" + PROVIDER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(json(validFields())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Provider " + PROVIDER_ID + " does not exist"));
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                arguments("blank name", json(with("name", "   "))),
                arguments("name longer than 200 characters", json(with("name", "x".repeat(201)))),
                arguments("missing name", json(without("name"))),
                arguments("missing category", json(without("categoryId"))),
                arguments("logo path longer than 500 characters", json(with("logoPath", "x".repeat(501)))));
    }

    private static ProviderResponse gymResponse() {
        return new ProviderResponse(PROVIDER_ID, NAME, "Gym", null, ProviderType.CUSTOM);
    }

    private static Map<String, Object> validFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("name", NAME);
        fields.put("categoryId", CATEGORY_ID);
        return fields;
    }

    private static Map<String, Object> with(String field, Object value) {
        Map<String, Object> fields = validFields();
        fields.put(field, value);
        return fields;
    }

    private static Map<String, Object> without(String field) {
        Map<String, Object> fields = validFields();
        fields.remove(field);
        return fields;
    }

    private static String json(Map<String, Object> fields) {
        return JSON.writeValueAsString(fields);
    }
}
