package com.app.subly.exception.subscription;

import java.time.LocalDate;

public class PaymentDateInPastException extends RuntimeException {
    public PaymentDateInPastException(LocalDate nextPaymentDate) {
        super("Next payment date " + nextPaymentDate + " is in the past");
    }
}
