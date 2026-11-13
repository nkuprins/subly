package com.app.subly.testdata;

import com.app.subly.entity.user.User;
import com.app.subly.enums.UserRole;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestUsers {

    public static final String ALICE = "alice";
    public static final String BOB = "bob";
    public static final String CAROL = "carol";
    public static final long ALICE_ID = 1L;
    public static final long BOB_ID = 2L;
    public static final String PASSWORD_HASH = "hash";

    public static User user(String username) {
        return new User(username, username, PASSWORD_HASH, email(username), UserRole.USER);
    }

    public static User userWithId(String username, long id) {
        User user = user(username);
        user.setId(id);
        return user;
    }

    public static String email(String username) {
        return username + "@example.com";
    }
}
