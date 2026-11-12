package com.app.subly.repository;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.CAROL;
import static com.app.subly.testdata.TestUsers.email;
import static com.app.subly.testdata.TestUsers.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.app.subly.entity.user.User;
import com.app.subly.exception.user.UserDoesNotExistException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class UserRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    UserRepository repository;

    @Test
    void saveAndFlush_shouldRejectDuplicateUsername() {
        repository.saveAndFlush(user(ALICE));
        User sameUsername = user(ALICE);
        sameUsername.setEmail(email(BOB));

        assertThatThrownBy(() -> repository.saveAndFlush(sameUsername))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_shouldRejectDuplicateEmail() {
        repository.saveAndFlush(user(ALICE));
        User sameEmail = user(BOB);
        sameEmail.setEmail(email(ALICE));

        assertThatThrownBy(() -> repository.saveAndFlush(sameEmail))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flush_shouldRejectEmailOfAnotherUser() {
        repository.saveAndFlush(user(ALICE));
        User bob = repository.saveAndFlush(user(BOB));

        bob.setEmail(email(ALICE));

        assertThatThrownBy(repository::flush).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flush_shouldRejectUsernameOfAnotherUser() {
        repository.saveAndFlush(user(ALICE));
        User bob = repository.saveAndFlush(user(BOB));

        bob.setUsername(ALICE);

        assertThatThrownBy(repository::flush).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void getUser_shouldReturnExistingUser() {
        repository.save(user(ALICE));

        assertThat(repository.getUser(ALICE).getEmail()).isEqualTo(email(ALICE));
    }

    @Test
    void getUser_shouldRejectUnknownUser() {
        assertThatThrownBy(() -> repository.getUser("unknown"))
                .isInstanceOf(UserDoesNotExistException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void findDistinctTimeZones_shouldReturnEachZoneOnce() {
        User carol = user(CAROL);
        carol.setTimeZone(TOKYO);
        repository.save(carol);
        repository.save(user(ALICE));
        repository.save(user(BOB));

        assertThat(repository.findDistinctTimeZones()).containsExactlyInAnyOrder(TOKYO, DEFAULT_ZONE);
    }
}
