package com.app.subly.exception.subscription;

import com.app.subly.exception.BadRequestException;

public class CatalogProviderReadOnlyException extends BadRequestException {
    public CatalogProviderReadOnlyException(String providerName) {
        super("Name and category of the catalog provider '" + providerName + "' are read-only");
    }
}
