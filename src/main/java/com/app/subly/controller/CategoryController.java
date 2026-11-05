package com.app.subly.controller;

import com.app.subly.dto.OperationResponse;
import com.app.subly.dto.subscription.CategoryResponse;
import com.app.subly.dto.subscription.CreateCustomCategoryRequest;
import com.app.subly.dto.subscription.UpdateCategoryRequest;
import com.app.subly.exceptions.subscription.*;
import com.app.subly.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service){
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<OperationResponse> createCategory(@Valid @RequestBody CreateCustomCategoryRequest request){
        try{
            this.service.addNewCategory(request.getCategoryName());
            return ResponseEntity.ok(
                    new OperationResponse(false, "Successfully added new category " + request.getCategoryName()));
        } catch (CategoryNotFoundException | CategoryAlreadyExistsException ex){
            return ResponseEntity.badRequest().body(new OperationResponse(true, ex.getMessage()));
        }
    }

    @PutMapping("/edit")
    public ResponseEntity<OperationResponse> editCategory(@Valid @RequestBody UpdateCategoryRequest request){
        try{
            this.service.editCategoryName(request.getId(), request.getNewName());
            return ResponseEntity.ok(
                    new OperationResponse(false, "The category name has been changed successfully"));
        } catch (CategoryNotFoundException | CategoryAlreadyExistsException | CategoryUpdateException ex){
            return ResponseEntity.badRequest().body(new OperationResponse(true, ex.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<OperationResponse> deleteCategory(@PathVariable Long id){
        if(id == null || id < 0){
            return ResponseEntity.badRequest().body(new OperationResponse(true, "Id must be greater than 0"));
        }

        try {
            this.service.deleteCategory(id);
            return ResponseEntity.ok(
                    new OperationResponse(false, "The category has been deleted successfully"));
        } catch (CategoryDeletionException | CategoryNotFoundException ex){
            return ResponseEntity.badRequest().body(new OperationResponse(true, ex.getMessage()));
        }
    }

    @GetMapping("/listAll")
    public List<CategoryResponse> getAllCategories(){
        return this.service.getAllCategories();
    }
}
