package com.siddharth.cashapp.exception;

/**
 * Thrown when a customer, invoice or payment id does not exist. Becomes HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
