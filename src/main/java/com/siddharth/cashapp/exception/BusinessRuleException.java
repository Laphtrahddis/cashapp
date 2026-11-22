package com.siddharth.cashapp.exception;

/**
 * Thrown when a request is valid JSON but breaks a business rule,
 * e.g. a duplicate invoice number or matching a payment that is already fully applied.
 * Becomes HTTP 409 Conflict.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
