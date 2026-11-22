package com.siddharth.cashapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siddharth.cashapp.model.Customer;

/**
 * Spring Data writes the implementation at startup: save, findById, findAll, delete, ...
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
