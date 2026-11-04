package com.app.subly.repository;

import com.app.subly.models.User;
import com.app.subly.models.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;



@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private TestEntityManager manager;

    @Test
    public void save_ShouldAddUserCorrectly(){
        assertDoesNotThrow(()
                -> repository.save(createUser("andris123", "andris@gmail.com", "Andris")));

        Optional<User> user = repository.findByUsername("andris123");

        assertTrue(user.isPresent());
        assertThat(user.get().getUsername()).isEqualTo("andris123");
        assertThat(user.get().getEmail()).isEqualTo("andris@gmail.com");
        assertThat(user.get().getName()).isEqualTo("Andris");
    }

    @Test
    public void save_ShouldFailIfUsernameAlreadyExists(){
        User firstUser = createUser("andris123", "andris@gmail.com", "Andris");
        User secondUser = createUser("andris123", "alex@gmail.com", "Alex");

        assertDoesNotThrow(()
                -> repository.saveAndFlush(firstUser));

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(secondUser));

    }

    @Test
    public void save_ShouldFailIfEmailAlreadyExists(){
        User firstUser = createUser("andris123", "andris@gmail.com", "Andris");
        User secondUser = createUser("alex123", "andris@gmail.com", "Alex");

        assertDoesNotThrow(()
                -> repository.saveAndFlush(firstUser));

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(secondUser));

    }

    @Test
    void shouldUpdateUserEmail() {
        User user = createUser("andris123", "andris@gmail.com", "Andris");

        repository.save(user);

        user.setEmail("new_andris_email@gmail.com");
        repository.flush();
        manager.clear();

        Optional<User> storedUser = repository.findByUsername("andris123");

        assertTrue(storedUser.isPresent());
        assertThat(storedUser.get().getEmail()).isEqualTo("new_andris_email@gmail.com");
    }

    @Test
    void shouldUpdateUsername() {
        User user = createUser("andris123", "andris@gmail.com", "Andris");

        repository.save(user);

        user.setUsername("andris");
        repository.flush();
        manager.clear();

        Optional<User> storedUser = repository.findByUsername("andris");

        assertTrue(storedUser.isPresent());
        assertThat(storedUser.get().getUsername()).isEqualTo("andris");
    }

    @Test
    void update_ShouldFailIfEmailAlreadyExists() {
        User firstUser = createUser("andris123", "andris@gmail.com", "Andris");
        User secondUser = createUser("alex123", "alex@gmail.com", "Alex");

        repository.saveAndFlush(firstUser);
        repository.saveAndFlush(secondUser);

        secondUser.setEmail("andris@gmail.com");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.flush()
        );
    }

    @Test
    void update_ShouldFailIfUsernameAlreadyExists() {
        User firstUser = createUser("andris123", "andris@gmail.com", "Andris");
        User secondUser = createUser("alex123", "alex@gmail.com", "Alex");

        repository.saveAndFlush(firstUser);
        repository.saveAndFlush(secondUser);

        secondUser.setUsername("andris123");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.flush()
        );
    }

    @Test
    void findByUsername_ShouldReturnEmptyIfUserDoesNotExist() {
        Optional<User> user = repository.findByUsername("unknown");

        assertThat(user).isEmpty();
    }

    private User createUser(String username, String email, String name){
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("test_password");
        user.setRole(UserRole.USER);

        return user;
    }
}
