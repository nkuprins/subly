package com.app.subly.dto.dashboard;

import com.app.subly.dto.MoneyResponse;
import java.time.YearMonth;

public record PaidMonthResponse(YearMonth month, MoneyResponse paid) {}
