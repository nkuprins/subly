package com.app.subly.repository;

import com.app.subly.models.subscription.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
