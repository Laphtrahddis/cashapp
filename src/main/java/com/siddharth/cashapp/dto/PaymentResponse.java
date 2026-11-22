package com.siddharth.cashapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.siddharth.cashapp.model.PaymentStatus;

/**
 * What the API returns for a payment, including where its money went so far.
 */
public record PaymentResponse(
        Long id,
        Long customerId,
        BigDecimal amount,
        BigDecimal unappliedAmount,
        LocalDate paymentDate,
        String referenceText,
        PaymentStatus status,
        List<PaymentApplicationResponse> applications
) {
}
