package com.app.subly.exception.category;

import com.app.subly.exception.ConflictException;

public class CategoryAlreadyExistsException extends ConflictException {
    public CategoryAlreadyExistsException(String categoryName) {
        super("A category with the '" + categoryName + "' name already exists");
    }
}
