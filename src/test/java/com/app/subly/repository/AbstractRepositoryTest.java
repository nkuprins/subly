package com.app.subly.repository;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;

import com.app.subly.entity.category.Category;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.user.User;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AbstractRepositoryTest {

    @Autowired
    TestEntityManager entityManager;

    User persistUser(String username) {
        return entityManager.persist(TestUsers.user(username));
    }

    Category persistCategory() {
        return entityManager.persist(TestProviders.category());
    }

    Subscription persistSubscription(User providerCreator) {
        SubscriptionProvider provider = entityManager.persist(TestProviders.custom(persistCategory(), providerCreator));
        return entityManager.persist(aSubscription().withProvider(provider).build());
    }
}
