package com.siddharth.cashapp.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.siddharth.cashapp.dto.CreateCustomerRequest;
import com.siddharth.cashapp.dto.CustomerResponse;
import com.siddharth.cashapp.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // @Valid runs the checks on CreateCustomerRequest; failures go to GlobalExceptionHandler (400).
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse createdCustomer = customerService.createCustomer(request);
        URI location = URI.create("/customers/" + createdCustomer.id());
        return ResponseEntity.created(location).body(createdCustomer);   // 201 Created
    }

    @GetMapping
    public List<CustomerResponse> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable Long customerId) {
        return customerService.getCustomer(customerId);
    }
}
