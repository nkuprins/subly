package com.app.subly.entity.user;

import com.app.subly.enums.UserRole;
import jakarta.persistence.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Europe/Riga");

    public static final int USERNAME_MAX_LENGTH = 100;

    public static final int NAME_MAX_LENGTH = 100;

    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(unique = true, nullable = false, length = USERNAME_MAX_LENGTH)
    private String username;

    @Setter
    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Setter
    @Column(nullable = false)
    private String passwordHash;

    @Setter
    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Setter
    @Column(nullable = false, length = 64)
    private ZoneId timeZone = DEFAULT_TIME_ZONE;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    public User(String username, String name, String passwordHash, String email, UserRole role) {
        this.username = username;
        this.name = name;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
    }

    public LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(timeZone));
    }
}
