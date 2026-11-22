package com.siddharth.cashapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siddharth.cashapp.model.PaymentApplication;

public interface PaymentApplicationRepository extends JpaRepository<PaymentApplication, Long> {

    // All the pieces of one payment, in the order they were applied.
    List<PaymentApplication> findByPaymentIdOrderByIdAsc(Long paymentId);
}
