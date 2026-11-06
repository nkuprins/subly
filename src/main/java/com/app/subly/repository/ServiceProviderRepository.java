package com.app.subly.repository;

import com.app.subly.model.subscription.ServiceProvider;
import com.app.subly.model.subscription.ServiceType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {

    Optional<ServiceProvider> findByIdAndServiceType(Long id, ServiceType serviceType);

    @EntityGraph(attributePaths = "category")
    List<ServiceProvider> findAllByServiceTypeOrderByNameAsc(ServiceType serviceType);
}
