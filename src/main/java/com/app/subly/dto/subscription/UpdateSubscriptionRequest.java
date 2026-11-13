package com.app.subly.dto.subscription;

import com.app.subly.enums.BillingCycle;
import com.app.subly.util.MoneyMath;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@AllArgsConstructor
@ToString
public class UpdateSubscriptionRequest {

    @NotNull
    @Positive
    @Digits(integer = MoneyMath.INTEGER_DIGITS, fraction = MoneyMath.FRACTION_DIGITS)
    private BigDecimal price;

    @NotNull
    private BillingCycle billingCycle;

    @NotNull
    private LocalDate nextPaymentDate;

    private boolean freeTrial;
}
