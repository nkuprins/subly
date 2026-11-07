package com.app.subly.controller;

import com.app.subly.dto.OperationResponse;
import com.app.subly.dto.subscription.CategoryResponse;
import com.app.subly.dto.subscription.CreateCustomCategoryRequest;
import com.app.subly.dto.subscription.UpdateCategoryRequest;
import com.app.subly.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @PostMapping("/create")
    public OperationResponse createCategory(@Valid @RequestBody CreateCustomCategoryRequest request) {
        service.addNewCategory(request.getCategoryName());
        return new OperationResponse("Successfully added new category " + request.getCategoryName());
    }

    @PutMapping("/edit")
    public OperationResponse editCategory(@Valid @RequestBody UpdateCategoryRequest request) {
        service.editCategoryName(request.getId(), request.getNewName());
        return new OperationResponse("The category name has been changed successfully");
    }

    @DeleteMapping("/delete/{id}")
    public OperationResponse deleteCategory(@PathVariable @Positive Long id) {
        service.deleteCategory(id);
        return new OperationResponse("The category has been deleted successfully");
    }

    @GetMapping("/listAll")
    public List<CategoryResponse> getAllCategories() {
        return service.getAllCategories();
    }
}
