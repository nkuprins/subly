package com.app.subly.exceptions.subscription;

public class CatalogProviderReadOnlyException extends RuntimeException {
    public CatalogProviderReadOnlyException(String providerName) {
        super("Name and category of the catalog provider '" + providerName + "' are read-only");
    }
}
