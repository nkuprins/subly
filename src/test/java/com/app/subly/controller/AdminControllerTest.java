package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.app.subly.dto.admin.AdminStatisticsResponse;
import com.app.subly.dto.category.CategoryResponse;
import com.app.subly.dto.form.CreateCategoryForm;
import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.dto.provider.ProviderCategoryResponse;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.enums.Currency;
import com.app.subly.enums.ProviderType;
import com.app.subly.exception.category.CategoryAlreadyExistsException;
import com.app.subly.service.AdminStatisticsService;
import com.app.subly.service.CategoryService;
import com.app.subly.service.ProviderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AdminController.class)
@WithMockUser(username = ALICE, roles = "ADMIN")
class AdminControllerTest extends AbstractWebMvcTest {

    private static final String ADMIN_URL = "/admin";
    private static final String CATEGORY_URL = "/admin/category/create";
    private static final String PROVIDER_URL = "/admin/providers";

    @MockitoBean
    private ProviderService providerService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private AdminStatisticsService adminStatisticsService;

    @Test
    void catalogue_shouldPopulateStatisticsProvidersAndCategoryCounts() throws Exception {
        stubCatalogue();
        SubscriptionProvider provider = mock(SubscriptionProvider.class, RETURNS_DEEP_STUBS);
        when(provider.getName()).thenReturn("Netflix");
        when(provider.getCategory().getName()).thenReturn("Streaming");
        when(providerService.findCatalogProviders()).thenReturn(List.of(provider));

        mockMvc.perform(get(ADMIN_URL).param("providerCreated", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("providers", List.of(provider)))
                .andExpect(model().attribute("providerCount", 1))
                .andExpect(model().attribute("providerCreated", true))
                .andExpect(model().attribute(
                                "categories",
                                List.of(new CategoryResponse(1L, "Streaming"), new CategoryResponse(2L, "Music"))))
                .andExpect(model().attribute("categoryReferences", Map.of(1L, 2L, 2L, 0L)))
                .andExpect(model().attribute("statistics", statistics()))
                .andExpect(model().attributeExists("categoryCreationForm"))
                .andExpect(model().attribute("categoryCreationError", ""));

        verify(providerService).findCatalogProviders();
        verify(providerService).findProviderCountsByCategory();
        verify(categoryService).findAll();
        verify(adminStatisticsService).getStatistics();
    }

    @Test
    void catalogue_shouldRenderAnEmptyCatalogue() throws Exception {
        when(providerService.findCatalogProviders()).thenReturn(List.of());
        when(categoryService.findAll()).thenReturn(List.of());
        when(providerService.findProviderCountsByCategory()).thenReturn(List.of());
        when(adminStatisticsService.getStatistics()).thenReturn(statistics());

        mockMvc.perform(get(ADMIN_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("providerCount", 0))
                .andExpect(model().attribute("providerCreated", false))
                .andExpect(model().attribute("categoryReferences", Map.of()))
                .andExpect(model().attribute("categories", List.of()));
    }

    @Test
    void catalogue_shouldPreserveCategoryFormAndErrorAfterRedirect() throws Exception {
        stubCatalogue();
        CreateCategoryForm form = new CreateCategoryForm();
        form.setTitle("Music");

        mockMvc.perform(get(ADMIN_URL)
                        .flashAttr("categoryCreationForm", form)
                        .flashAttr("categoryCreationError", "Already exists"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("providerCreated", false))
                .andExpect(model().attribute("categoryCreationForm", form))
                .andExpect(model().attribute("categoryCreationError", "Already exists"));
    }

    @Test
    void newProvider_shouldRenderFormWithCategories() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(new CategoryResponse(1L, "Streaming")));

        mockMvc.perform(get("/admin/providers/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-provider-form"))
                .andExpect(model().attributeExists("providerForm"))
                .andExpect(model().attribute("categories", List.of(new CategoryResponse(1L, "Streaming"))));

        verify(categoryService).findAll();
        verifyNoInteractions(providerService, adminStatisticsService);
    }

    @Test
    void addCategory_shouldRedirectAfterSuccessfulCreation() throws Exception {
        mockMvc.perform(post(CATEGORY_URL).with(csrf()).param("title", "Music"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ADMIN_URL));

        verify(categoryService).create("Music");
        verifyNoInteractions(providerService, adminStatisticsService);
    }

    @Test
    void addCategory_shouldRedirectWithValidationError() throws Exception {
        mockMvc.perform(post(CATEGORY_URL).with(csrf()).param("title", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ADMIN_URL))
                .andExpect(flash().attributeExists("categoryCreationForm"))
                .andExpect(flash().attribute("categoryCreationError", "You must provide a valid, non-empty title"));

        verifyNoInteractions(categoryService, providerService, adminStatisticsService);
    }

    @Test
    void addCategory_shouldShowErrorWhenCategoryAlreadyExists() throws Exception {
        doThrow(new CategoryAlreadyExistsException("Music"))
                .when(categoryService)
                .create("Music");

        mockMvc.perform(post(CATEGORY_URL).with(csrf()).param("title", "Music"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ADMIN_URL))
                .andExpect(flash().attributeExists("categoryCreationForm"))
                .andExpect(flash().attribute("categoryCreationError", "The category with this name already exists"));

        verify(categoryService).create("Music");
    }

    @Test
    void addNewProvider_shouldRedirectAfterSuccessfulCreation() throws Exception {
        mockMvc.perform(post(PROVIDER_URL)
                        .with(csrf())
                        .param("name", "Netflix")
                        .param("categoryId", "1")
                        .param("logoPath", "/images/netflix.svg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ADMIN_URL));

        verify(providerService).create(eq(ALICE), any(CreateProviderForm.class), eq(ProviderType.CATALOG));
        verifyNoInteractions(adminStatisticsService);
    }

    @Test
    void addNewProvider_shouldReturn400AndRenderFormWhenInputIsInvalid() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(new CategoryResponse(1L, "Streaming")));

        mockMvc.perform(post(PROVIDER_URL).with(csrf()).param("name", " ").param("categoryId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("admin-provider-form"))
                .andExpect(model().attributeHasFieldErrors("providerForm", "name", "categoryId"))
                .andExpect(model().attribute("categories", List.of(new CategoryResponse(1L, "Streaming"))));

        verify(categoryService).findAll();
        verifyNoInteractions(providerService, adminStatisticsService);
    }

    @Test
    void getStatistics_shouldReturnJsonForAdmin() throws Exception {
        when(adminStatisticsService.getStatistics()).thenReturn(statistics());

        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.activeSubscriptions").value(2))
                .andExpect(jsonPath("$.averageMonthlySpendPerUser").value(18.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.popularServices").isArray());

        verify(adminStatisticsService).getStatistics();
        verifyNoInteractions(providerService, categoryService);
    }

    @Test
    @WithMockUser(username = ALICE, roles = "USER")
    void catalogue_shouldReturn403ForRegularUser() throws Exception {
        mockMvc.perform(get(ADMIN_URL)).andExpect(status().isForbidden());

        verifyNoInteractions(providerService, categoryService, adminStatisticsService);
    }

    @Test
    @WithMockUser(username = ALICE, roles = "USER")
    void getStatistics_shouldReturn403ForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/statistics")).andExpect(status().isForbidden());

        verifyNoInteractions(adminStatisticsService);
    }

    @Test
    @WithMockUser(username = ALICE, roles = "USER")
    void addCategory_shouldReturn403ForRegularUser() throws Exception {
        mockMvc.perform(post(CATEGORY_URL).with(csrf()).param("title", "Music")).andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser(username = ALICE, roles = "USER")
    void addNewProvider_shouldReturn403ForRegularUser() throws Exception {
        mockMvc.perform(post(PROVIDER_URL).with(csrf()).param("name", "Netflix").param("categoryId", "1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(providerService);
    }

    @Test
    @WithAnonymousUser
    void catalogue_shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get(ADMIN_URL))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() == 302
                        || result.getResponse().getStatus() == 401));

        verifyNoInteractions(providerService, categoryService, adminStatisticsService);
    }

    @Test
    void addCategory_shouldRejectMissingCsrfToken() throws Exception {
        mockMvc.perform(post(CATEGORY_URL).param("title", "Music")).andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void addNewProvider_shouldRejectMissingCsrfToken() throws Exception {
        mockMvc.perform(post(PROVIDER_URL).param("name", "Netflix").param("categoryId", "1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(providerService);
    }

    private void stubCatalogue() {
        when(providerService.findCatalogProviders()).thenReturn(List.of());
        when(providerService.findProviderCountsByCategory()).thenReturn(List.of(new ProviderCategoryResponse(1L, 2L)));
        when(categoryService.findAll())
                .thenReturn(List.of(new CategoryResponse(1L, "Streaming"), new CategoryResponse(2L, "Music")));
        when(adminStatisticsService.getStatistics()).thenReturn(statistics());
    }

    private static AdminStatisticsResponse statistics() {
        return new AdminStatisticsResponse(3L, 2L, new BigDecimal("18.00"), Currency.EUR, List.of());
    }
}
