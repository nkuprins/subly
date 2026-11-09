package com.app.subly.controller;

import static com.app.subly.model.provider.SubscriptionProvider.LOGO_PATH_MAX_LENGTH;
import static com.app.subly.model.provider.SubscriptionProvider.NAME_MAX_LENGTH;
import static com.app.subly.testdata.TestProviders.CATEGORY_NAME;
import static com.app.subly.testdata.TestProviders.CUSTOM_PROVIDER_NAME;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.exception.category.InvalidCategoryException;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.service.ProviderService;
import com.app.subly.testutil.JsonBody;
import com.app.subly.testutil.ValidationCases;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ProviderController.class)
@WithMockUser(username = ALICE)
class ProviderControllerTest extends AbstractWebMvcTest {

    static final String URL = "/api/providers";
    static final long PROVIDER_ID = 3L;
    static final long CATEGORY_ID = 4L;
    static final String LOGO_PATH = "/logo.svg";
    static final String ITEM_URL = URL + "/" + PROVIDER_ID;
    static final ProviderRequest REQUEST = new ProviderRequest(CUSTOM_PROVIDER_NAME, CATEGORY_ID, null);
    static final JsonBody VALID_BODY =
            JsonBody.empty().with("name", CUSTOM_PROVIDER_NAME).with("categoryId", CATEGORY_ID);
    static final ValidationCases CREATE = new ValidationCases("create", HttpMethod.POST, URL, VALID_BODY);
    static final ValidationCases UPDATE = new ValidationCases("update", HttpMethod.PUT, ITEM_URL, VALID_BODY);

    @MockitoBean
    ProviderService service;

    @Test
    void create_shouldReturn201WithTheCustomProvider() throws Exception {
        when(service.create(ALICE, REQUEST, ProviderType.CUSTOM)).thenReturn(providerResponse());

        sendJson(post(URL), VALID_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROVIDER_ID))
                .andExpect(jsonPath("$.name").value(CUSTOM_PROVIDER_NAME))
                .andExpect(jsonPath("$.categoryName").value(CATEGORY_NAME))
                .andExpect(jsonPath("$.logoPath").value(LOGO_PATH))
                .andExpect(jsonPath("$.providerType").value("CUSTOM"));
    }

    @Test
    void create_shouldReturn400ForUnknownCategory() throws Exception {
        InvalidCategoryException exception = new InvalidCategoryException(CATEGORY_ID);
        when(service.create(anyString(), any(), any())).thenThrow(exception);

        sendJson(post(URL), VALID_BODY).andExpect(problem(HttpStatus.BAD_REQUEST, exception));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void createAndUpdate_shouldRejectInvalidFields(
            String description, MockHttpServletRequestBuilder request, JsonBody body, String field) throws Exception {
        sendJson(request, body)
                .andExpect(validationError())
                .andExpect(jsonPath("$.errors[0].field").value(field));

        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bodiesAtMaxLength")
    void createAndUpdate_shouldAcceptFieldsOfMaxLength(
            String description, MockHttpServletRequestBuilder request, JsonBody body) throws Exception {
        sendJson(request, body).andExpect(status().is2xxSuccessful());
    }

    @Test
    void listVisible_shouldReturnProvidersVisibleToTheUser() throws Exception {
        when(service.findVisibleTo(ALICE)).thenReturn(List.of(providerResponse()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(PROVIDER_ID));
    }

    @Test
    void update_shouldReturn200WithTheProvider() throws Exception {
        when(service.updateCustomProvider(ALICE, PROVIDER_ID, REQUEST)).thenReturn(providerResponse());

        sendJson(put(ITEM_URL), VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROVIDER_ID));
    }

    @Test
    void update_shouldReturn400ForProviderTheUserDoesNotOwn() throws Exception {
        ProviderNotFoundException exception = new ProviderNotFoundException(PROVIDER_ID);
        when(service.updateCustomProvider(anyString(), anyLong(), any())).thenThrow(exception);

        sendJson(put(ITEM_URL), VALID_BODY).andExpect(problem(HttpStatus.BAD_REQUEST, exception));
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                CREATE.with("name", "   ", "blank"),
                CREATE.with("name", "x".repeat(NAME_MAX_LENGTH + 1), "too long"),
                CREATE.without("name"),
                CREATE.without("categoryId"),
                CREATE.with("logoPath", "x".repeat(LOGO_PATH_MAX_LENGTH + 1), "too long"),
                UPDATE.with("name", "   ", "blank"),
                UPDATE.with("name", "x".repeat(NAME_MAX_LENGTH + 1), "too long"),
                UPDATE.without("name"),
                UPDATE.without("categoryId"),
                UPDATE.with("logoPath", "x".repeat(LOGO_PATH_MAX_LENGTH + 1), "too long"));
    }

    static Stream<Arguments> bodiesAtMaxLength() {
        JsonBody body =
                VALID_BODY.with("name", "x".repeat(NAME_MAX_LENGTH)).with("logoPath", "x".repeat(LOGO_PATH_MAX_LENGTH));
        return Stream.of(arguments("create", post(URL), body), arguments("update", put(ITEM_URL), body));
    }

    private static ProviderResponse providerResponse() {
        return new ProviderResponse(PROVIDER_ID, CUSTOM_PROVIDER_NAME, CATEGORY_NAME, LOGO_PATH, ProviderType.CUSTOM);
    }
}
