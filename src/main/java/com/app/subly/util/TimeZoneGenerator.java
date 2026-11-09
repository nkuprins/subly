package com.app.subly.util;

import java.time.ZoneId;
import java.util.List;

public class TimeZoneGenerator {
    public static List<String> getAllFilteredTimeZone() {
        return ZoneId.getAvailableZoneIds().stream()
                .filter((zone) -> zone.startsWith("Europe") || zone.startsWith("Asia") || zone.startsWith("America"))
                .sorted()
                .toList();
    }
}
