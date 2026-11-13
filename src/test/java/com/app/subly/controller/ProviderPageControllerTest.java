package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.app.subly.dto.category.CategoryResponse;
import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.enums.ProviderType;
import com.app.subly.service.CategoryService;
import com.app.subly.service.ProviderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ProviderPageController.class)
@WithMockUser(username = ALICE)
class ProviderPageControllerTest extends AbstractWebMvcTest {

    private static final String NEW_PROVIDER_URL = "/providers/new";
    private static final String PROVIDERS_URL = "/providers";
    private static final List<CategoryResponse> CATEGORIES = List.of(new CategoryResponse(1L, "Music"));

    @MockitoBean
    CategoryService categoryService;

    @MockitoBean
    ProviderService providerService;

    @Test
    void createProvider_shouldRedirectWhenProviderIsCreated() throws Exception {
        mockMvc.perform(post(PROVIDERS_URL)
                        .with(csrf())
                        .param("name", "Custom provider")
                        .param("categoryId", "1")
                        .param("logoPath", "https://example.com/logo.png"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/subscriptions/new"));

        verify(providerService).create(eq(ALICE), any(CreateProviderForm.class), eq(ProviderType.CUSTOM));
    }

    @Test
    void createProvider_shouldReturn400WhenFormIsInvalid() throws Exception {
        when(categoryService.findAll()).thenReturn(CATEGORIES);

        mockMvc.perform(post(PROVIDERS_URL)
                        .with(csrf())
                        .param("name", "")
                        .param("categoryId", "1")
                        .param("logoPath", ""))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(view().name("provider-form"))
                .andExpect(model().attribute("categories", CATEGORIES))
                .andExpect(model().attributeHasFieldErrors("providerForm", "name"));

        verifyNoInteractions(providerService);
        verify(categoryService).findAll();
    }

    @Test
    void newProvider_shouldReturnFormAndCategories() throws Exception {
        when(categoryService.findAll()).thenReturn(CATEGORIES);

        mockMvc.perform(get(NEW_PROVIDER_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("provider-form"))
                .andExpect(model().attributeExists("providerForm"))
                .andExpect(model().attribute("categories", CATEGORIES));

        verify(categoryService).findAll();
        verifyNoInteractions(providerService);
    }
}
