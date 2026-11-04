package com.app.subly.services;

import com.app.subly.exceptions.subscription.CategoryNotFoundException;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.ServiceProvider;
import com.app.subly.models.subscription.ServiceType;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final ServiceProviderRepository serviceProviderRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ServiceProvider> findCatalogServices() {
        return serviceProviderRepository.findAllByServiceTypeOrderByNameAsc(ServiceType.CATALOG);
    }

    @Transactional(readOnly = true)
    public List<Category> findCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public void createCatalogService(String name, Long categoryId, String logoPath) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        ServiceProvider service = ServiceProvider.catalog(name.strip(), category);
        service.setLogoPath(logoPath == null || logoPath.isBlank() ? null : logoPath.strip());
        serviceProviderRepository.save(service);
    }
}
