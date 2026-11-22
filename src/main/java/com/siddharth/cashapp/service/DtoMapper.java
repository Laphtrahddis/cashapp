package com.siddharth.cashapp.service;

import java.util.ArrayList;
import java.util.List;

import com.siddharth.cashapp.dto.CustomerResponse;
import com.siddharth.cashapp.dto.InvoiceResponse;
import com.siddharth.cashapp.dto.PaymentApplicationResponse;
import com.siddharth.cashapp.dto.PaymentResponse;
import com.siddharth.cashapp.model.Customer;
import com.siddharth.cashapp.model.Invoice;
import com.siddharth.cashapp.model.Payment;
import com.siddharth.cashapp.model.PaymentApplication;

/**
 * Converts database entities into the response records the API sends.
 * Keeping this in one place means entities never leak out of the service layer.
 */
public final class DtoMapper {

    private DtoMapper() {
        // only static methods, no objects
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail());
    }

    public static InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getCustomer().getId(),
                invoice.getTotalAmount(),
                invoice.getOpenAmount(),
                invoice.getDueDate(),
                invoice.getStatus());
    }

    public static PaymentApplicationResponse toApplicationResponse(PaymentApplication application) {
        return new PaymentApplicationResponse(
                application.getInvoice().getInvoiceNumber(),
                application.getAmountApplied(),
                application.getMatchRule(),
                application.getAppliedAt());
    }

    public static List<PaymentApplicationResponse> toApplicationResponses(List<PaymentApplication> applications) {
        List<PaymentApplicationResponse> responses = new ArrayList<>();
        for (PaymentApplication application : applications) {
            responses.add(toApplicationResponse(application));
        }
        return responses;
    }

    public static PaymentResponse toPaymentResponse(Payment payment, List<PaymentApplication> applications) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCustomer().getId(),
                payment.getAmount(),
                payment.getUnappliedAmount(),
                payment.getPaymentDate(),
                payment.getReferenceText(),
                payment.getStatus(),
                toApplicationResponses(applications));
    }
}
