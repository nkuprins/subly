package com.app.subly.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// The DTO that is used for simple JSON responses in the Create, Delete and Update operations.

@Getter
@AllArgsConstructor
public class OperationResponse {
    private String message;
}
