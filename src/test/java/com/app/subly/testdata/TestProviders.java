package com.app.subly.testdata;

import com.app.subly.entity.category.Category;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.user.User;

public final class TestProviders {

    public static final String CATEGORY_NAME = "Test category";
    public static final String CATALOG_PROVIDER_NAME = "Test catalog provider";
    public static final String CUSTOM_PROVIDER_NAME = "Test custom provider";

    private TestProviders() {}

    public static Category category() {
        return new Category(CATEGORY_NAME);
    }

    public static SubscriptionProvider catalog(Category category) {
        return SubscriptionProvider.catalog(CATALOG_PROVIDER_NAME, category);
    }

    public static SubscriptionProvider custom(Category category, User creator) {
        return SubscriptionProvider.custom(CUSTOM_PROVIDER_NAME, category, creator);
    }
}
