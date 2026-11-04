package com.app.subly.repository;

import com.app.subly.models.subscription.ServiceProvider;
import com.app.subly.models.subscription.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {

    Optional<ServiceProvider> findByIdAndServiceType(Long id, ServiceType serviceType);
}
