package com.app.subly.service;

import com.app.subly.dto.provider.ProviderCategoryResponse;
import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.entity.category.Category;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.enums.ProviderType;
import com.app.subly.exception.category.InvalidCategoryException;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final SubscriptionProviderRepository providerRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionProvider> findCatalogProviders() {
        return providerRepository.findAllByProviderTypeOrderByNameAsc(ProviderType.CATALOG);
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> findVisibleTo(String username) {
        return providerRepository
                .findAllByProviderTypeOrCreatedByUsernameOrderByNameAsc(ProviderType.CATALOG, username)
                .stream()
                .map(ProviderResponse::from)
                .toList();
    }

    @Transactional
    public ProviderResponse create(String username, ProviderRequest request, ProviderType type) {
        Category category = findCategory(request.categoryId());
        String name = request.name().strip();
        SubscriptionProvider provider = (type == ProviderType.CATALOG)
                ? SubscriptionProvider.catalog(name, category)
                : SubscriptionProvider.custom(name, category, userRepository.getByUsername(username));
        provider.setLogoPath(normalizeLogoPath(request.logoPath()));
        return ProviderResponse.from(providerRepository.save(provider));
    }

    @Transactional
    public ProviderResponse updateCustomProvider(String username, long providerId, ProviderRequest request) {
        SubscriptionProvider provider = providerRepository
                .findByIdAndCreatedByUsername(providerId, username)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
        provider.setCategory(findCategory(request.categoryId()));
        provider.setName(request.name().strip());
        provider.setLogoPath(normalizeLogoPath(request.logoPath()));
        return ProviderResponse.from(provider);
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
        return providerRepository.getAllProvidersCountGroupedByCategory();
    }

    private Category findCategory(long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new InvalidCategoryException(categoryId));
    }

    private static String normalizeLogoPath(String logoPath) {
        return logoPath == null || logoPath.isBlank() ? null : logoPath.strip();
    }
}
