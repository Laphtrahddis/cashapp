package com.siddharth.cashapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One line of the audit trail: "this much of payment P went to invoice I, decided by rule R".
 * One payment can pay several invoices, and one invoice can be paid by several payments.
 */
@Entity
@Table(name = "payment_applications")
public class PaymentApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountApplied;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchRule matchRule;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    protected PaymentApplication() {
    }

    public PaymentApplication(Payment payment, Invoice invoice, BigDecimal amountApplied, MatchRule matchRule) {
        this.payment = payment;
        this.invoice = invoice;
        this.amountApplied = amountApplied;
        this.matchRule = matchRule;
        this.appliedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Payment getPayment() {
        return payment;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public BigDecimal getAmountApplied() {
        return amountApplied;
    }

    public MatchRule getMatchRule() {
        return matchRule;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }
}
