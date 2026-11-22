package com.siddharth.cashapp.dto;

/**
 * What the API returns for a customer.
 */
public record CustomerResponse(Long id, String name, String email) {
}
