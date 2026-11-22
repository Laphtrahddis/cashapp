package com.siddharth.cashapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.siddharth.cashapp.model.InvoiceStatus;

/**
 * What the API returns for an invoice.
 */
public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long customerId,
        BigDecimal totalAmount,
        BigDecimal openAmount,
        LocalDate dueDate,
        InvoiceStatus status
) {
}
