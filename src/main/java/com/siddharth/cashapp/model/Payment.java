package com.siddharth.cashapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 * Money received from a customer. unappliedAmount is the part not yet matched to any invoice.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unappliedAmount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    // The remittance note the customer sent with the money, e.g. "Paying INV-1001 and INV-1002".
    @Column(length = 500)
    private String referenceText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    protected Payment() {
    }

    public Payment(Customer customer, BigDecimal amount, LocalDate paymentDate, String referenceText) {
        this.customer = customer;
        this.amount = amount;
        this.unappliedAmount = amount;   // nothing applied yet
        this.paymentDate = paymentDate;
        this.referenceText = referenceText;
        this.status = PaymentStatus.UNMATCHED;
    }

    /**
     * Marks part of this payment as used to pay an invoice and updates the status.
     */
    public void useAmount(BigDecimal amountUsed) {
        if (amountUsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount used must be positive");
        }
        if (amountUsed.compareTo(unappliedAmount) > 0) {
            throw new IllegalArgumentException(
                    "Cannot use " + amountUsed + ", only " + unappliedAmount + " is unapplied");
        }

        unappliedAmount = unappliedAmount.subtract(amountUsed);

        if (unappliedAmount.compareTo(BigDecimal.ZERO) == 0) {
            status = PaymentStatus.FULLY_APPLIED;
        } else {
            status = PaymentStatus.PARTIALLY_APPLIED;
        }
    }

    public boolean hasMoneyLeft() {
        return unappliedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getUnappliedAmount() {
        return unappliedAmount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getReferenceText() {
        return referenceText;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}
