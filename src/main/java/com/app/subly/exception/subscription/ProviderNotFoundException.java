package com.app.subly.exception.subscription;

import com.app.subly.exception.BadRequestException;

public class ProviderNotFoundException extends BadRequestException {
    public ProviderNotFoundException(long providerId) {
        super("Provider " + providerId + " does not exist");
    }
}
