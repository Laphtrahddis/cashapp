package com.siddharth.cashapp.dto;

import java.math.BigDecimal;
import java.util.List;

import com.siddharth.cashapp.model.PaymentStatus;

/**
 * Result of POST /payments/{id}/match: what this matching run applied, and what money is left.
 */
public record MatchResult(
        Long paymentId,
        List<PaymentApplicationResponse> applications,
        BigDecimal unappliedAmount,
        PaymentStatus paymentStatus
) {
}
