package com.siddharth.cashapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siddharth.cashapp.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
