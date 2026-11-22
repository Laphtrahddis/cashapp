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
 * A bill we sent to a customer. openAmount is what the customer still owes on it.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal openAmount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    protected Invoice() {
    }

    public Invoice(String invoiceNumber, Customer customer, BigDecimal totalAmount, LocalDate dueDate) {
        this.invoiceNumber = invoiceNumber;
        this.customer = customer;
        this.totalAmount = totalAmount;
        this.openAmount = totalAmount;   // a new invoice is fully unpaid
        this.dueDate = dueDate;
        this.status = InvoiceStatus.OPEN;
    }

    /**
     * Reduces what the customer owes on this invoice and updates the status.
     */
    public void applyPayment(BigDecimal amountToApply) {
        if (amountToApply.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to apply must be positive");
        }
        if (amountToApply.compareTo(openAmount) > 0) {
            throw new IllegalArgumentException(
                    "Cannot apply " + amountToApply + " to invoice " + invoiceNumber
                            + ", only " + openAmount + " is open");
        }

        openAmount = openAmount.subtract(amountToApply);

        if (openAmount.compareTo(BigDecimal.ZERO) == 0) {
            status = InvoiceStatus.PAID;
        } else {
            status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public boolean isStillOpen() {
        return status != InvoiceStatus.PAID;
    }

    public Long getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getOpenAmount() {
        return openAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }
}
