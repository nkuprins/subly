package com.app.subly.repository;

import com.app.subly.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    void deleteByUsername(String username);

    @Query("select distinct u.timeZone from User u")
    List<ZoneId> findDistinctTimeZones();
}
