package com.app.subly.service;

import com.app.subly.dto.subscription.CategoryResponse;
import com.app.subly.exception.subscription.*;
import com.app.subly.model.subscription.Category;
import com.app.subly.repository.CategoryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository repository;
    private final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
        this.createDefaultCategories();
    }

    @Transactional
    public void addNewCategory(String categoryName) {
        Category category = new Category(categoryName);

        try {
            this.repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException(categoryName);
        }
    }

    @Transactional
    public void editCategoryName(Long id, String newCategoryName) {
        Category category = this.repository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));

        if (category.getName().equals(newCategoryName)) {
            throw new CategoryUpdateException("The new name must differ from the old name");
        }

        try {
            category.setName(newCategoryName);
            this.repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException(newCategoryName);
        }
    }

    public void deleteCategory(Long id) {
        Category category = this.repository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));

        try {
            this.repository.delete(category);
        } catch (OptimisticLockingFailureException ex) {
            throw new CategoryDeletionException(
                    "Cannot delete the category because some table's field references to it");
        }
    }

    public List<CategoryResponse> getAllCategories() {
        return this.repository.findAll().stream()
                .map((category) -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }

    private void createDefaultCategories() {
        logger.info("Checking default categories...");
        String[] categories = new String[] {"Music", "Gaming", "Health", "Streaming", "Sport", "Other"};

        for (String categoryName : categories) {
            try {
                this.addNewCategory(categoryName);
                logger.info("Category {}: CREATED", categoryName);
            } catch (CategoryAlreadyExistsException ex) {
                logger.info("Category {}: OK", categoryName);
            }
        }
    }
}
