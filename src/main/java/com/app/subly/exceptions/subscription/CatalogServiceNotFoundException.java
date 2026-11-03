package com.app.subly.exceptions.subscription;

public class CatalogServiceNotFoundException extends RuntimeException {
    public CatalogServiceNotFoundException(long serviceId) {
        super("Catalog service " + serviceId + " does not exist");
    }
}
