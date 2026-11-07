package com.app.subly.exception.category;

import com.app.subly.exception.BadRequestException;

public class CategoryUpdateException extends BadRequestException {
    public CategoryUpdateException(String message) {
        super(message);
    }
}
