package com.app.subly.service;

import com.app.subly.exception.category.InvalidCategoryException;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.ProviderType;
import com.app.subly.model.subscription.SubscriptionProvider;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final SubscriptionProviderRepository subscriptionProviderRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionProvider> findCatalogProviders() {
        return subscriptionProviderRepository.findAllByProviderTypeOrderByNameAsc(ProviderType.CATALOG);
    }

    @Transactional(readOnly = true)
    public List<Category> findCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public void createCatalogProvider(String name, Long categoryId, String logoPath) {
        Category category =
                categoryRepository.findById(categoryId).orElseThrow(() -> new InvalidCategoryException(categoryId));
        SubscriptionProvider provider = SubscriptionProvider.catalog(name.strip(), category);
        provider.setLogoPath(logoPath == null || logoPath.isBlank() ? null : logoPath.strip());
        subscriptionProviderRepository.save(provider);
    }
}
