package com.app.subly.testdata;

import com.app.subly.entity.user.User;
import com.app.subly.enums.UserRole;

// Idea: use builder like in subscription if benefits
public final class TestUsers {

    public static final String ALICE = "alice";
    public static final String BOB = "bob";
    public static final String CAROL = "carol";
    public static final String PASSWORD_HASH = "hash";

    private TestUsers() {}

    public static User user(String username) {
        return user(username, UserRole.USER);
    }

    public static User user(String username, UserRole role) {
        return new User(username, username, PASSWORD_HASH, email(username), role);
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
