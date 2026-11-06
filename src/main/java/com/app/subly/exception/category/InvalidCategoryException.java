package com.app.subly.exception.category;

import com.app.subly.exception.BadRequestException;

public class InvalidCategoryException extends BadRequestException {
    public InvalidCategoryException(long categoryId) {
        super("Category " + categoryId + " does not exist");
    }
}
