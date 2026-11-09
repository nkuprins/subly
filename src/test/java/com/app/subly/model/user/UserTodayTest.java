package com.app.subly.model.user;

import static com.app.subly.testdata.TestClock.CLOCK;
import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestClock.TOKYO_TODAY;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.testdata.TestUsers;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class UserTodayTest {

    @Test
    void today_shouldReturnTheDateInTheUsersTimeZone() {
        assertThat(userIn(DEFAULT_ZONE).today(CLOCK)).isEqualTo(TODAY);
        assertThat(userIn(TOKYO).today(CLOCK)).isEqualTo(TOKYO_TODAY);
    }

    private static User userIn(ZoneId zone) {
        User user = TestUsers.user(ALICE);
        user.setTimeZone(zone);
        return user;
    }
}
