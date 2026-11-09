package com.app.subly.service;

import com.app.subly.dto.category.CategoryResponse;
import com.app.subly.exception.category.*;
import com.app.subly.model.category.Category;
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
        String name = categoryName.strip();
        Category category = new Category(name);

        try {
            repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException(name);
        }
    }

    @Transactional
    public void editCategoryName(Long id, String newCategoryName) {
        Category category = repository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        String newName = newCategoryName.strip();

        if (category.getName().equals(newName)) {
            throw new CategoryUpdateException("The new name must differ from the old name");
        }

        try {
            category.setName(newName);
            repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryAlreadyExistsException(newName);
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
