package com.siddharth.cashapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.siddharth.cashapp.model.MatchRule;

/**
 * One piece of a payment applied to one invoice, and the rule that decided it.
 */
public record PaymentApplicationResponse(
        String invoiceNumber,
        BigDecimal amountApplied,
        MatchRule matchRule,
        LocalDateTime appliedAt
) {
}
