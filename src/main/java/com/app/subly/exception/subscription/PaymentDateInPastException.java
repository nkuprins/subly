package com.app.subly.exception.subscription;

import com.app.subly.exception.BadRequestException;
import java.time.LocalDate;

public class PaymentDateInPastException extends BadRequestException {
    public PaymentDateInPastException(LocalDate nextPaymentDate) {
        super("Next payment date " + nextPaymentDate + " is in the past");
    }
}
