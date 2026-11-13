package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.exception.category.CategoryAlreadyExistsException;
import com.app.subly.exception.category.CategoryDeletionException;
import com.app.subly.exception.category.CategoryNotFoundException;
import com.app.subly.exception.category.CategoryUpdateException;
import com.app.subly.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(CategoryController.class)
@WithMockUser(username = ALICE, roles = "ADMIN")
class CategoryControllerTest extends AbstractWebMvcTest {

    private static final String EDIT_URL = "/api/category/edit";
    private static final String DELETE_URL = "/api/category/delete/%d";
    private static final long CATEGORY_ID = 1L;
    private static final String NEW_CATEGORY_NAME = "newCategory";

    @MockitoBean
    CategoryService service;

    @Test
    void editCategory_shouldReturn200WhenCategoryIsEdited() throws Exception {
        sendJson(put(EDIT_URL).with(csrf()), editCategory(CATEGORY_ID, NEW_CATEGORY_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("The category name has been changed successfully"));

        verify(service).rename(CATEGORY_ID, NEW_CATEGORY_NAME);
    }

    @Test
    void editCategory_shouldReturn400WhenNewNameIsSameAsCurrentName() throws Exception {
        CategoryUpdateException exception = new CategoryUpdateException("The new name must differ from the old name");
        doThrow(exception).when(service).rename(CATEGORY_ID, NEW_CATEGORY_NAME);

        sendJson(put(EDIT_URL).with(csrf()), editCategory(CATEGORY_ID, NEW_CATEGORY_NAME))
                .andExpect(problem(HttpStatus.BAD_REQUEST, exception));
    }

    @Test
    void editCategory_shouldReturn404WhenCategoryDoesNotExist() throws Exception {
        CategoryNotFoundException exception = new CategoryNotFoundException(CATEGORY_ID);
        doThrow(exception).when(service).rename(CATEGORY_ID, NEW_CATEGORY_NAME);

        sendJson(put(EDIT_URL).with(csrf()), editCategory(CATEGORY_ID, NEW_CATEGORY_NAME))
                .andExpect(problem(HttpStatus.NOT_FOUND, exception));
    }

    @Test
    void deleteCategory_shouldReturn200WhenCategoryIsDeleted() throws Exception {
        sendJson(delete(deleteUrl(CATEGORY_ID)).with(csrf()), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("The category has been deleted successfully"));

        verify(service).delete(CATEGORY_ID);
    }

    @Test
    void deleteCategory_shouldReturn404WhenCategoryDoesNotExist() throws Exception {
        CategoryNotFoundException exception = new CategoryNotFoundException(CATEGORY_ID);
        doThrow(exception).when(service).delete(CATEGORY_ID);

        sendJson(delete(deleteUrl(CATEGORY_ID)).with(csrf()), "").andExpect(problem(HttpStatus.NOT_FOUND, exception));
    }

    @Test
    void deleteCategory_shouldReturn409WhenCategoryCannotBeDeleted() throws Exception {
        CategoryDeletionException exception =
                new CategoryDeletionException("Cannot delete the category because some table's field references to it");
        doThrow(exception).when(service).delete(CATEGORY_ID);

        sendJson(delete(deleteUrl(CATEGORY_ID)).with(csrf()), "").andExpect(problem(HttpStatus.CONFLICT, exception));
    }

    @Test
    void editCategory_shouldReturn409WhenNewNameAlreadyExists() throws Exception {
        CategoryAlreadyExistsException exception = new CategoryAlreadyExistsException(NEW_CATEGORY_NAME);
        doThrow(exception).when(service).rename(CATEGORY_ID, NEW_CATEGORY_NAME);

        sendJson(put(EDIT_URL).with(csrf()), editCategory(CATEGORY_ID, NEW_CATEGORY_NAME))
                .andExpect(problem(HttpStatus.CONFLICT, exception));
    }

    private static String editCategory(long id, String newName) {
        return """
                {"id": %d, "newName": "%s"}
                """.formatted(id, newName);
    }

    private static String deleteUrl(long id) {
        return DELETE_URL.formatted(id);
    }
}
