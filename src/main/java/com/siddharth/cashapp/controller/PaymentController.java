package com.siddharth.cashapp.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siddharth.cashapp.dto.CreatePaymentRequest;
import com.siddharth.cashapp.dto.MatchResult;
import com.siddharth.cashapp.dto.PaymentResponse;
import com.siddharth.cashapp.service.MatchingService;
import com.siddharth.cashapp.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final MatchingService matchingService;

    public PaymentController(PaymentService paymentService, MatchingService matchingService) {
        this.paymentService = paymentService;
        this.matchingService = matchingService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse createdPayment = paymentService.createPayment(request);
        URI location = URI.create("/payments/" + createdPayment.id());
        return ResponseEntity.created(location).body(createdPayment);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable Long paymentId) {
        return paymentService.getPayment(paymentId);
    }

    // Runs the matching rules for this payment. POST because it changes data.
    @PostMapping("/{paymentId}/match")
    public MatchResult matchPayment(@PathVariable Long paymentId) {
        return matchingService.matchPayment(paymentId);
    }
}
