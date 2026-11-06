package com.app.subly.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class UserTodayTest {

    // CLOCK is 16:00 UTC on 4 Nov: still 4 Nov in Riga, but already 5 Nov in Tokyo.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T16:00:00Z"), ZoneOffset.UTC);

    @Test
    void todayIsTheDateInTheUsersTimeZone() {
        assertThat(userIn("Europe/Riga").today(CLOCK)).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(userIn("Asia/Tokyo").today(CLOCK)).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    private static User userIn(String zone) {
        User user = new User();
        user.setTimeZone(ZoneId.of(zone));
        return user;
    }
}
