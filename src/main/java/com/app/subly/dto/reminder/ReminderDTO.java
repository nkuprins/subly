package com.app.subly.dto.reminder;

import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class ReminderDTO {
    private Long reminderId;

    private String email;
    private String subscriptionProviderName;
    private boolean isFreeTrial;

    private BigDecimal subscriptionPrice;
    private Currency paymentCurrency;
    private BillingCycle billingCycle;

    private LocalDate subscriptionNextPaymentDate;
    private LocalDate firstSentAt;
    private LocalDate secondSentAt;
}
