package com.app.subly.exception.subscription;

import com.app.subly.exception.BadRequestException;
import java.math.BigDecimal;

public class PriceTooLowException extends BadRequestException {
    public PriceTooLowException(BigDecimal price, int maxParticipants, BigDecimal minimum) {
        super("Price " + price + " is too low for " + maxParticipants + " participants, the minimum is " + minimum);
    }
}
