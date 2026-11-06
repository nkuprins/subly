package com.app.subly.exception.subscription;

public class CatalogServiceNotFoundException extends RuntimeException {
    public CatalogServiceNotFoundException(long serviceId) {
        super("Catalog service " + serviceId + " does not exist");
    }
}
