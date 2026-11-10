package com.app.subly.dto;

import com.app.subly.model.subscription.Currency;
import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, Currency currency) {}
