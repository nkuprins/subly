package com.app.subly.service;

import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.dto.provider.ProviderCategoryResponse;
import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.entity.category.Category;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.enums.ProviderType;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final SubscriptionProviderRepository providerRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(ProviderService.class);

    @Transactional(readOnly = true)
    public List<SubscriptionProvider> findCatalogProviders() {
        logger.debug("Finding catalogue providers");
        List<SubscriptionProvider> providers =
                providerRepository.findAllByProviderTypeOrderByNameAsc(ProviderType.CATALOG);
        logger.debug("Found {} catalogue providers", providers.size());
        return providers;
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> findVisibleTo(String username) {
        logger.debug("Getting visible providers for '{}'", username);
        return providerRepository
                .findAllByProviderTypeOrCreatedByUsernameOrderByNameAsc(ProviderType.CATALOG, username)
                .stream()
                .map(ProviderResponse::from)
                .toList();
    }

    @Transactional
    public void create(String username, CreateProviderForm form, ProviderType type) {
        logger.debug("Creating provider for username={} type={}", username, type);
        Category category = categoryRepository.getCategory(form.getCategoryId());
        String name = form.getName().strip();
        SubscriptionProvider provider = (type == ProviderType.CATALOG)
                ? SubscriptionProvider.catalog(name, category)
                : SubscriptionProvider.custom(name, category, userRepository.getByUsername(username));
        provider.setLogoPath(normalizeLogoPath(form.getLogoPath()));
        providerRepository.save(provider);
        logger.debug("Created provider for username={} type={}", username, type);
    }

    @Transactional
    public void deleteAllProvidersCreatedBy(String username) {
        List<ProviderResponse> result = findVisibleTo(username);
        if (result.isEmpty()) {
            return;
        }

        for (ProviderResponse response : result) {
            if (response.providerType().equals(ProviderType.CUSTOM)) providerRepository.deleteById(response.id());
        }
    }

    public List<ProviderCategoryResponse> findProviderCountsByCategory() {
        logger.debug("Finding provider counts by category");
        return providerRepository.getAllProvidersCountGroupedByCategory();
    }

    private static String normalizeLogoPath(String logoPath) {
        return logoPath == null || logoPath.isBlank() ? null : logoPath.strip();
    }
}
