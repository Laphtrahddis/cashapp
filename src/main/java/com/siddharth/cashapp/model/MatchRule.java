package com.siddharth.cashapp.model;

/**
 * Which matching rule decided that a piece of a payment goes to an invoice.
 */
public enum MatchRule {
    REFERENCE_MATCH,  // the customer wrote the invoice number in the payment reference
    EXACT_AMOUNT,     // exactly one open invoice has the same amount as the payment
    OLDEST_FIRST      // fallback: pay the invoice with the earliest due date first
}
