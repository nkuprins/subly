package com.app.subly.exception.subscription;

public class CatalogServiceReadOnlyException extends RuntimeException {
    public CatalogServiceReadOnlyException(String serviceName) {
        super("Name and category of the catalog service '" + serviceName + "' are read-only");
    }
}
