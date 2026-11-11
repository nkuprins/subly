package com.app.subly.enums;

import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillingCycle {
    MONTHLY(ChronoUnit.MONTHS, 12),
    YEARLY(ChronoUnit.YEARS, 1);

    private final ChronoUnit unit;
    private final int paymentsPerYear;
}
