package com.app.subly.exceptions.subscription;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(long categoryId) {
        super("Category with the id " + categoryId + " does not exist");
    }
}
