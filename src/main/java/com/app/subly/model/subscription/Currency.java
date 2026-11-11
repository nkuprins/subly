package com.app.subly.model.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Currency {
    EUR("€"),
    USD("$"),
    GBP("£");

    private final String symbol;
}
