package com.app.subly.exception.subscription;

import com.app.subly.exception.BadRequestException;

public class CatalogProviderNotFoundException extends BadRequestException {
    public CatalogProviderNotFoundException(long providerId) {
        super("Catalog provider " + providerId + " does not exist");
    }
}
