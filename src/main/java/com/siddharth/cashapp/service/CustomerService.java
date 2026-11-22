package com.siddharth.cashapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siddharth.cashapp.dto.CreateCustomerRequest;
import com.siddharth.cashapp.dto.CustomerResponse;
import com.siddharth.cashapp.exception.ResourceNotFoundException;
import com.siddharth.cashapp.model.Customer;
import com.siddharth.cashapp.repository.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    // Constructor injection: Spring passes in the repository when it creates this service.
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        Customer customer = new Customer(request.name(), request.email());
        Customer savedCustomer = customerRepository.save(customer);
        return DtoMapper.toCustomerResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        List<CustomerResponse> responses = new ArrayList<>();
        for (Customer customer : customerRepository.findAll()) {
            responses.add(DtoMapper.toCustomerResponse(customer));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        return DtoMapper.toCustomerResponse(findCustomerOrThrow(customerId));
    }

    /**
     * Used by the other services when an invoice or payment points to a customer.
     */
    public Customer findCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + customerId + " not found"));
    }
}
