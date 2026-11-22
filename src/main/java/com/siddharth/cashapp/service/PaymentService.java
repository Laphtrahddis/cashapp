package com.siddharth.cashapp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siddharth.cashapp.dto.CreatePaymentRequest;
import com.siddharth.cashapp.dto.PaymentResponse;
import com.siddharth.cashapp.exception.ResourceNotFoundException;
import com.siddharth.cashapp.model.Customer;
import com.siddharth.cashapp.model.Payment;
import com.siddharth.cashapp.model.PaymentApplication;
import com.siddharth.cashapp.repository.PaymentApplicationRepository;
import com.siddharth.cashapp.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentApplicationRepository paymentApplicationRepository;
    private final CustomerService customerService;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentApplicationRepository paymentApplicationRepository,
                          CustomerService customerService) {
        this.paymentRepository = paymentRepository;
        this.paymentApplicationRepository = paymentApplicationRepository;
        this.customerService = customerService;
    }

    /**
     * Records money received. It is NOT matched yet; that happens in POST /payments/{id}/match.
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Customer customer = customerService.findCustomerOrThrow(request.customerId());
        Payment payment = new Payment(customer, request.amount(), request.paymentDate(), request.referenceText());
        Payment savedPayment = paymentRepository.save(payment);
        return DtoMapper.toPaymentResponse(savedPayment, List.of());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + paymentId + " not found"));
        List<PaymentApplication> applications = paymentApplicationRepository.findByPaymentIdOrderByIdAsc(paymentId);
        return DtoMapper.toPaymentResponse(payment, applications);
    }
}
