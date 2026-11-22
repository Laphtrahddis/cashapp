package com.siddharth.cashapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * JSON body for POST /invoices.
 */
public record CreateInvoiceRequest(

        @NotBlank(message = "invoiceNumber is required")
        String invoiceNumber,

        @NotNull(message = "customerId is required")
        Long customerId,

        @NotNull(message = "totalAmount is required")
        @Positive(message = "totalAmount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "totalAmount can have at most 2 decimal places")
        BigDecimal totalAmount,

        @NotNull(message = "dueDate is required")
        LocalDate dueDate
) {
}
