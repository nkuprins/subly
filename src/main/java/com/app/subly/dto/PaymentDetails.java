package com.app.subly.dto;

import com.app.subly.model.subscription.BillingCycle;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PaymentDetails {

    BigDecimal price();

    BillingCycle billingCycle();

    LocalDate nextPaymentDate();

    Boolean freeTrial();
}
