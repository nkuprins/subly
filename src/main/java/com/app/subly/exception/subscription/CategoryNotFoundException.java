package com.app.subly.exception.subscription;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(long categoryId) {
        super("Category " + categoryId + " does not exist");
    }
}
