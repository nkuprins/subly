package com.app.subly.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.model.User;
import com.app.subly.model.UserRole;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.ServiceProvider;
import com.app.subly.model.subscription.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceProviderRepositoryTest {

    @Autowired
    ServiceProviderRepository serviceProviderRepository;

    @Autowired
    TestEntityManager entityManager;

    Category gym;

    @BeforeEach
    void setUp() {
        gym = entityManager.persist(new Category("Test gym"));
    }

    @Test
    void findByIdAndServiceTypeReturnsCatalogService() {
        ServiceProvider catalog = entityManager.persistFlushFind(ServiceProvider.catalog("Test catalog", gym));

        assertThat(serviceProviderRepository.findByIdAndServiceType(catalog.getId(), ServiceType.CATALOG))
                .map(ServiceProvider::getName)
                .contains("Test catalog");
    }

    @Test
    void findByIdAndServiceTypeSkipsCustomServiceWhenAskedForCatalog() {
        User creator = new User();
        creator.setUsername("test-owner");
        creator.setName("Test Owner");
        creator.setEmail("test-owner@test.local");
        creator.setPasswordHash("not-a-real-hash");
        creator.setRole(UserRole.USER);
        entityManager.persist(creator);
        ServiceProvider custom = entityManager.persistFlushFind(ServiceProvider.custom("My gym", gym, creator));

        assertThat(serviceProviderRepository.findByIdAndServiceType(custom.getId(), ServiceType.CATALOG))
                .isEmpty();
    }
}
