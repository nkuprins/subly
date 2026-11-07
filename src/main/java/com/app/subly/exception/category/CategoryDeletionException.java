package com.app.subly.exception.category;

import com.app.subly.exception.ConflictException;

public class CategoryDeletionException extends ConflictException {
    public CategoryDeletionException(String message) {
        super(message);
    }
}
