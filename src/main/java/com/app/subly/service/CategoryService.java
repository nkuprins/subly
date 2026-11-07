package com.app.subly.service;

import com.app.subly.dto.subscription.CategoryResponse;
import com.app.subly.exception.category.*;
import com.app.subly.model.subscription.Category;
import com.app.subly.repository.CategoryRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
        createDefaultCategories();
    }

    @Transactional
    public void addNewCategory(String categoryName) {
        Category category = new Category(categoryName);

        try {
            repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException(categoryName);
        }
    }

    @Transactional
    public void editCategoryName(Long id, String newCategoryName) {
        Category category = repository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));

        if (category.getName().equals(newCategoryName)) {
            throw new CategoryUpdateException("The new name must differ from the old name");
        }

        try {
            category.setName(newCategoryName);
            repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException(newCategoryName);
        }
    }

    public void deleteCategory(Long id) {
        Category category = repository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));

        try {
            repository.delete(category);
        } catch (OptimisticLockingFailureException ex) {
            throw new CategoryDeletionException(
                    "Cannot delete the category because some table's field references to it");
        }
    }

    public List<CategoryResponse> getAllCategories() {
        return repository.findAll().stream()
                .map((category) -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Category> findCategories() {
        return repository.findAllByOrderByNameAsc();
    }

    private void createDefaultCategories() {
        log.info("Checking default categories...");
        String[] categories = {"Music", "Gaming", "Health", "Streaming", "Sport", "Other"};

        for (String categoryName : categories) {
            try {
                addNewCategory(categoryName);
                log.info("Category {}: CREATED", categoryName);
            } catch (CategoryAlreadyExistsException ex) {
                log.info("Category {}: OK", categoryName);
            }
        }
    }
}
