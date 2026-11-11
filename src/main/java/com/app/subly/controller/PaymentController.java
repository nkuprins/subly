package com.app.subly.controller;

import com.app.subly.dto.payment.PaymentResponse;
import com.app.subly.service.PaymentService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public List<PaymentResponse> listMine(Principal principal) {
        return paymentService.findHistory(principal.getName());
    }
}

// MAYBE DELETE
