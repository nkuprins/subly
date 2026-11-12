package com.app.subly.testdata;

import com.app.subly.entity.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestClock {

    public static final Instant NOW = Instant.parse("2026-11-04T16:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final ZoneId DEFAULT_ZONE = User.DEFAULT_TIME_ZONE;
    public static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    public static final LocalDate TOKYO_TODAY = LocalDate.of(2026, 11, 5);
}
