package com.app.subly.service;

import com.app.subly.exception.subscription.CategoryNotFoundException;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.ServiceProvider;
import com.app.subly.model.subscription.ServiceType;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.ServiceProviderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Category category =
                categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
        ServiceProvider service = ServiceProvider.catalog(name.strip(), category);
        service.setLogoPath(logoPath == null || logoPath.isBlank() ? null : logoPath.strip());
        serviceProviderRepository.save(service);
    }
}
