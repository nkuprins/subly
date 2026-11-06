package com.app.subly.exception.subscription;

public class CatalogProviderNotFoundException extends RuntimeException {
    public CatalogProviderNotFoundException(long providerId) {
        super("Catalog provider " + providerId + " does not exist");
    }
}
