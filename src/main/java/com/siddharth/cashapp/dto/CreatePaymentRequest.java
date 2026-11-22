package com.siddharth.cashapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * JSON body for POST /payments.
 */
public record CreatePaymentRequest(

        @NotNull(message = "customerId is required")
        Long customerId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "amount can have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "paymentDate is required")
        LocalDate paymentDate,

        // Optional remittance note, e.g. "Payment for INV-1001"
        @Size(max = 500, message = "referenceText can be at most 500 characters")
        String referenceText
) {
}
