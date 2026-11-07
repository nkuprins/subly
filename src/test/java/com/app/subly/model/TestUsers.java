package com.app.subly.model;

public final class TestUsers {

    private TestUsers() {}

    public static User user(String username) {
        return user(username, UserRole.USER);
    }

    public static User user(String username, UserRole role) {
        return new User(username, username, "hash", username + "@test.local", role);
    }
}
