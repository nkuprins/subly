package com.app.subly.repository;

import com.app.subly.entity.category.Category;
import com.app.subly.exception.category.CategoryNotFoundException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAsc();

    default Category getCategory(Long id) {
        return findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }
}
