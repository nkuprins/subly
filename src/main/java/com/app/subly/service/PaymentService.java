package com.app.subly.service;

import com.app.subly.dto.payment.PaymentResponse;
import com.app.subly.repository.SubscriptionPaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionPaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<PaymentResponse> findHistory(String username) {
        return paymentRepository.findHistoryOf(username).stream()
                .map(PaymentResponse::from)
                .toList();
    }
}
