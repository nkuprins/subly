package com.app.subly.repository;

import com.app.subly.entity.user.User;
import com.app.subly.exception.user.UserDoesNotExistException;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    default User getUser(String username) {
        return findByUsername(username)
                .orElseThrow(() -> new UserDoesNotExistException("User " + username + " does not exist"));
    }

    Optional<User> findByEmail(String email);

    void deleteByUsername(String username);

    @Query("select distinct u.timeZone from User u")
    List<ZoneId> findDistinctTimeZones();
}
