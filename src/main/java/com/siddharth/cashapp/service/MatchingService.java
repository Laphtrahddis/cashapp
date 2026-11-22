package com.siddharth.cashapp.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siddharth.cashapp.dto.MatchResult;
import com.siddharth.cashapp.exception.BusinessRuleException;
import com.siddharth.cashapp.exception.ResourceNotFoundException;
import com.siddharth.cashapp.model.Invoice;
import com.siddharth.cashapp.model.InvoiceStatus;
import com.siddharth.cashapp.model.MatchRule;
import com.siddharth.cashapp.model.Payment;
import com.siddharth.cashapp.model.PaymentApplication;
import com.siddharth.cashapp.repository.InvoiceRepository;
import com.siddharth.cashapp.repository.PaymentApplicationRepository;
import com.siddharth.cashapp.repository.PaymentRepository;

/**
 * Cash application: decides which open invoices a payment pays.
 *
 * Rules, in order, using only the SAME customer's unpaid invoices:
 *   1. Reference match: invoice numbers written in the payment's reference text, in the order they appear.
 *   2. Exact amount:    if rule 1 applied nothing and exactly one invoice's open amount equals the payment.
 *   3. Oldest first:    any money still left goes to invoices by earliest due date.
 * Money left after all three rules stays on the payment as unapplied (a credit on the customer's account).
 */
@Service
public class MatchingService {

    private static final List<InvoiceStatus> UNPAID_STATUSES =
            List.of(InvoiceStatus.OPEN, InvoiceStatus.PARTIALLY_PAID);

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentApplicationRepository paymentApplicationRepository;

    public MatchingService(PaymentRepository paymentRepository,
                           InvoiceRepository invoiceRepository,
                           PaymentApplicationRepository paymentApplicationRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentApplicationRepository = paymentApplicationRepository;
    }

    /**
     * @Transactional: every change below (invoices, payment, applications) is saved together,
     * or, if anything throws, none of it is saved.
     */
    @Transactional
    public MatchResult matchPayment(Long paymentId) {
        // Step 1: load the payment and make sure it still has money to apply
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + paymentId + " not found"));

        if (!payment.hasMoneyLeft()) {
            throw new BusinessRuleException("Payment " + paymentId + " is already fully applied");
        }

        // Step 2: load this customer's unpaid invoices, oldest due date first
        Long customerId = payment.getCustomer().getId();
        List<Invoice> unpaidInvoices =
                invoiceRepository.findByCustomerIdAndStatusInOrderByDueDateAscIdAsc(customerId, UNPAID_STATUSES);

        // Step 3: run the three rules in order; each one adds to newApplications
        List<PaymentApplication> newApplications = new ArrayList<>();

        applyReferenceMatches(payment, unpaidInvoices, newApplications);

        if (newApplications.isEmpty()) {
            applyExactAmountMatch(payment, unpaidInvoices, newApplications);
        }

        applyOldestFirst(payment, unpaidInvoices, newApplications);

        // Step 4: report what happened in this run
        return new MatchResult(
                payment.getId(),
                DtoMapper.toApplicationResponses(newApplications),
                payment.getUnappliedAmount(),
                payment.getStatus());
    }

    // Rule 1: pay the invoices the customer named in the reference text, in the order they were named.
    private void applyReferenceMatches(Payment payment, List<Invoice> unpaidInvoices,
                                       List<PaymentApplication> newApplications) {
        String referenceText = payment.getReferenceText();
        if (referenceText == null || referenceText.isBlank()) {
            return;
        }

        // Look up invoices by number, ignoring upper/lower case
        Map<String, Invoice> invoicesByNumber = new HashMap<>();
        for (Invoice invoice : unpaidInvoices) {
            invoicesByNumber.put(invoice.getInvoiceNumber().toUpperCase(), invoice);
        }

        // Split "Paying INV-1001, INV-1002." into words: ["Paying", "INV-1001", "INV-1002"]
        String[] words = referenceText.split("[^A-Za-z0-9-]+");
        for (String word : words) {
            if (!payment.hasMoneyLeft()) {
                return;
            }
            Invoice namedInvoice = invoicesByNumber.get(word.toUpperCase());
            if (namedInvoice != null && namedInvoice.isStillOpen()) {
                applyToInvoice(payment, namedInvoice, MatchRule.REFERENCE_MATCH, newApplications);
            }
        }
    }

    // Rule 2: if exactly one invoice owes exactly what the payment has left, it is almost certainly that one.
    private void applyExactAmountMatch(Payment payment, List<Invoice> unpaidInvoices,
                                       List<PaymentApplication> newApplications) {
        BigDecimal amountLeft = payment.getUnappliedAmount();
        List<Invoice> invoicesWithSameAmount = new ArrayList<>();

        for (Invoice invoice : unpaidInvoices) {
            // compareTo, not equals: for BigDecimal, 100.0 equals(100.00) is false
            if (invoice.isStillOpen() && invoice.getOpenAmount().compareTo(amountLeft) == 0) {
                invoicesWithSameAmount.add(invoice);
            }
        }

        // Two or more invoices with the same amount: we can't tell which one, so leave it to rule 3.
        if (invoicesWithSameAmount.size() == 1) {
            applyToInvoice(payment, invoicesWithSameAmount.get(0), MatchRule.EXACT_AMOUNT, newApplications);
        }
    }

    // Rule 3: spend whatever is left on the oldest invoices first (the list is already sorted by due date).
    private void applyOldestFirst(Payment payment, List<Invoice> unpaidInvoices,
                                  List<PaymentApplication> newApplications) {
        for (Invoice invoice : unpaidInvoices) {
            if (!payment.hasMoneyLeft()) {
                return;
            }
            if (invoice.isStillOpen()) {
                applyToInvoice(payment, invoice, MatchRule.OLDEST_FIRST, newApplications);
            }
        }
    }

    // Moves as much money as possible from the payment to one invoice and records it.
    private void applyToInvoice(Payment payment, Invoice invoice, MatchRule matchRule,
                                List<PaymentApplication> newApplications) {
        BigDecimal amountToApply = payment.getUnappliedAmount().min(invoice.getOpenAmount());

        invoice.applyPayment(amountToApply);
        payment.useAmount(amountToApply);

        // The invoice and payment changes are saved automatically at the end of the transaction
        // (JPA "dirty checking"); the new application row must be saved explicitly.
        PaymentApplication application = new PaymentApplication(payment, invoice, amountToApply, matchRule);
        newApplications.add(paymentApplicationRepository.save(application));
    }
}
