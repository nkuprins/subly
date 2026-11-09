package com.app.subly.exception.category;

import com.app.subly.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(long categoryId) {
        super("Category " + categoryId + " does not exist");
    }
}
