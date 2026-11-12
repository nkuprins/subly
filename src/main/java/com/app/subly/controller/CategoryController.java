package com.app.subly.controller;

import com.app.subly.dto.OperationResponse;
import com.app.subly.dto.category.UpdateCategoryRequest;
import com.app.subly.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService service;

    @PutMapping("/edit")
    public OperationResponse editCategory(@Valid @RequestBody UpdateCategoryRequest request) {
        log.info("Updating category id={}", request.id());
        service.rename(request.id(), request.newName());
        log.info("Successfully updated category id={}", request.id());
        return new OperationResponse("The category name has been changed successfully");
    }

    @DeleteMapping("/delete/{id}")
    public OperationResponse deleteCategory(@PathVariable @Positive Long id) {
        log.info("Deleting category id={}", id);
        service.delete(id);
        log.info("Successfully deleted category id={}", id);
        return new OperationResponse("The category has been deleted successfully");
    }
}
