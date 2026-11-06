package com.app.subly.exception.subscription;

public class CategoryAlreadyExistsException extends RuntimeException {
    public CategoryAlreadyExistsException(String categoryName) {
        super("A category with the '" + categoryName + "' name already exists");
    }
}
