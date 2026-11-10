package com.app.subly.service;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.PaidMonthResponse;
import com.app.subly.dto.payment.PaymentResponse;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.SubscriptionPayment;
import com.app.subly.repository.SubscriptionPaymentRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.util.CurrencyConverter;
import com.app.subly.util.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int PAID_MONTHS = 12;

    private final SubscriptionPaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PaymentResponse> findHistory(String username) {
        return paymentRepository.findHistoryOf(username).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaidMonthResponse> getPaidPerMonth(String username, Currency currency) {
        LocalDate today = userRepository.getByUsername(username).today(clock);
        YearMonth firstMonth = YearMonth.from(today).minusMonths(PAID_MONTHS - 1L);

        Map<YearMonth, BigDecimal> paid = new LinkedHashMap<>();
        for (int i = 0; i < PAID_MONTHS; i++) {
            paid.put(firstMonth.plusMonths(i), BigDecimal.ZERO);
        }

        for (SubscriptionPayment payment : paymentRepository.findPaidSince(username, firstMonth.atDay(1))) {
            BigDecimal amount = CurrencyConverter.convert(
                    payment.getAmount(), payment.getSubscription().getCurrency(), currency);
            paid.merge(YearMonth.from(payment.getPaidOn()), amount, BigDecimal::add);
        }

        List<PaidMonthResponse> result = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal> entry : paid.entrySet()) {
            MoneyResponse amount = new MoneyResponse(Money.round(entry.getValue()), currency);
            result.add(new PaidMonthResponse(entry.getKey(), amount));
        }
        return result;
    }
}
