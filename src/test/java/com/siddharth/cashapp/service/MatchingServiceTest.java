package com.siddharth.cashapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.siddharth.cashapp.dto.MatchResult;
import com.siddharth.cashapp.exception.BusinessRuleException;
import com.siddharth.cashapp.exception.ResourceNotFoundException;
import com.siddharth.cashapp.model.Customer;
import com.siddharth.cashapp.model.Invoice;
import com.siddharth.cashapp.model.InvoiceStatus;
import com.siddharth.cashapp.model.MatchRule;
import com.siddharth.cashapp.model.Payment;
import com.siddharth.cashapp.model.PaymentApplication;
import com.siddharth.cashapp.model.PaymentStatus;
import com.siddharth.cashapp.repository.InvoiceRepository;
import com.siddharth.cashapp.repository.PaymentApplicationRepository;
import com.siddharth.cashapp.repository.PaymentRepository;

/**
 * Unit tests: no Spring, no database. The repositories are Mockito mocks (fake objects
 * whose answers we script), so each test checks only the matching logic.
 */
@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long PAYMENT_ID = 99L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentApplicationRepository paymentApplicationRepository;

    // Mockito creates MatchingService and passes the three mocks into its constructor.
    @InjectMocks
    private MatchingService matchingService;

    private Customer customer;

    @BeforeEach
    void createCustomer() {
        customer = new Customer("Acme Ltd", "ap@acme.example");
        // Entities get their id from the database; there is no database here, so we set it directly.
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
    }

    @Test
    void exactAmountMatchPaysTheInvoiceWithTheSameAmount() {
        Invoice olderInvoice = createInvoice("INV-1", "500.00", LocalDate.of(2026, 1, 10));
        Invoice sameAmountInvoice = createInvoice("INV-2", "300.00", LocalDate.of(2026, 2, 10));
        Payment payment = createPayment("300.00", null);
        givenPaymentAndUnpaidInvoices(payment, List.of(olderInvoice, sameAmountInvoice));

        MatchResult result = matchingService.matchPayment(PAYMENT_ID);

        assertThat(result.applications()).hasSize(1);
        assertThat(result.applications().get(0).invoiceNumber()).isEqualTo("INV-2");
        assertThat(result.applications().get(0).matchRule()).isEqualTo(MatchRule.EXACT_AMOUNT);
        assertThat(sameAmountInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(olderInvoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);   // untouched, even though older
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.FULLY_APPLIED);
    }

    @Test
    void referenceMatchPaysNamedInvoicesInTheOrderTheyAreWritten() {
        Invoice invoiceA = createInvoice("INV-A", "200.00", LocalDate.of(2026, 1, 10));
        Invoice invoiceB = createInvoice("INV-B", "300.00", LocalDate.of(2026, 2, 10));
        Invoice invoiceC = createInvoice("INV-C", "400.00", LocalDate.of(2026, 3, 10));
        Payment payment = createPayment("700.00", "Paying inv-c and INV-B, thanks");
        givenPaymentAndUnpaidInvoices(payment, List.of(invoiceA, invoiceB, invoiceC));

        MatchResult result = matchingService.matchPayment(PAYMENT_ID);

        assertThat(result.applications()).hasSize(2);
        assertThat(result.applications().get(0).invoiceNumber()).isEqualTo("INV-C");
        assertThat(result.applications().get(0).amountApplied()).isEqualByComparingTo("400.00");
        assertThat(result.applications().get(1).invoiceNumber()).isEqualTo("INV-B");
        assertThat(result.applications().get(1).amountApplied()).isEqualByComparingTo("300.00");
        assertThat(result.applications().get(0).matchRule()).isEqualTo(MatchRule.REFERENCE_MATCH);
        assertThat(invoiceA.getStatus()).isEqualTo(InvoiceStatus.OPEN);   // not named, no money left for it
        assertThat(result.unappliedAmount()).isEqualByComparingTo("0");
    }

    @Test
    void partialPaymentLeavesInvoicePartiallyPaid() {
        Invoice invoice = createInvoice("INV-1", "1000.00", LocalDate.of(2026, 1, 10));
        Payment payment = createPayment("400.00", null);
        givenPaymentAndUnpaidInvoices(payment, List.of(invoice));

        MatchResult result = matchingService.matchPayment(PAYMENT_ID);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(invoice.getOpenAmount()).isEqualByComparingTo("600.00");
        assertThat(result.applications().get(0).matchRule()).isEqualTo(MatchRule.OLDEST_FIRST);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.FULLY_APPLIED);
    }

    @Test
    void overpaymentLeavesTheExtraMoneyUnapplied() {
        Invoice invoice = createInvoice("INV-1", "500.00", LocalDate.of(2026, 1, 10));
        Payment payment = createPayment("800.00", "INV-1");
        givenPaymentAndUnpaidInvoices(payment, List.of(invoice));

        MatchResult result = matchingService.matchPayment(PAYMENT_ID);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(result.unappliedAmount()).isEqualByComparingTo("300.00");
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_APPLIED);
    }

    @Test
    void oldestFirstPaysEarliestDueDateFirst() {
        // The repository returns invoices sorted by due date; the service must follow that order.
        Invoice januaryInvoice = createInvoice("INV-JAN", "100.00", LocalDate.of(2026, 1, 10));
        Invoice februaryInvoice = createInvoice("INV-FEB", "100.00", LocalDate.of(2026, 2, 10));
        Invoice marchInvoice = createInvoice("INV-MAR", "100.00", LocalDate.of(2026, 3, 10));
        Payment payment = createPayment("150.00", null);
        givenPaymentAndUnpaidInvoices(payment, List.of(januaryInvoice, februaryInvoice, marchInvoice));

        MatchResult result = matchingService.matchPayment(PAYMENT_ID);

        assertThat(januaryInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(februaryInvoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(februaryInvoice.getOpenAmount()).isEqualByComparingTo("50.00");
        assertThat(marchInvoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(result.applications()).hasSize(2);
    }

    @Test
    void twoInvoicesWithTheSameAmountFallBackToOldestFirst() {
        Invoice olderInvoice = createInvoice("INV-1", "100.00", LocalDate.of(2026, 1, 10));
        Invoice newerInvoice = createInvoice("INV-2", "100.00", LocalDate.of(2026, 2, 10));
        Payment payment = createPayment("100.00", null);
        givenPaymentAndUnpaidInvoices(payment, List.of(olderInvoice, newerInvoice));

        MatchResult result = matchingService.matchPayment(PAYMENT_ID);

        assertThat(result.applications().get(0).invoiceNumber()).isEqualTo("INV-1");
        assertThat(result.applications().get(0).matchRule()).isEqualTo(MatchRule.OLDEST_FIRST);
        assertThat(newerInvoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void otherCustomersInvoicesAreIgnored() {
        // The reference names another customer's invoice. The repository is asked only for
        // THIS customer's invoices, which here are none, so nothing may be applied.
        Payment payment = createPayment("250.00", "Paying INV-OTHER-CUSTOMER");
        givenPaymentAndUnpaidInvoices(payment, List.of());

        MatchResult result = matchingService.matchPayment(PAYMENT_ID);

        verify(invoiceRepository).findByCustomerIdAndStatusInOrderByDueDateAscIdAsc(eq(CUSTOMER_ID), anyCollection());
        assertThat(result.applications()).isEmpty();
        assertThat(result.unappliedAmount()).isEqualByComparingTo("250.00");
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.UNMATCHED);
    }

    @Test
    void fullyAppliedPaymentCannotBeMatchedAgain() {
        Payment payment = createPayment("100.00", null);
        payment.useAmount(new BigDecimal("100.00"));   // already used up
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> matchingService.matchPayment(PAYMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already fully applied");

        verify(paymentApplicationRepository, never()).save(any());
    }

    @Test
    void missingPaymentThrowsNotFound() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.matchPayment(PAYMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- helpers ----------

    private Invoice createInvoice(String invoiceNumber, String amount, LocalDate dueDate) {
        return new Invoice(invoiceNumber, customer, new BigDecimal(amount), dueDate);
    }

    private Payment createPayment(String amount, String referenceText) {
        Payment payment = new Payment(customer, new BigDecimal(amount), LocalDate.of(2026, 3, 1), referenceText);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        return payment;
    }

    // Scripts the mocks: "find payment 99 → this payment", "unpaid invoices → this list",
    // "save(application) → return the same application".
    private void givenPaymentAndUnpaidInvoices(Payment payment, List<Invoice> unpaidInvoices) {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findByCustomerIdAndStatusInOrderByDueDateAscIdAsc(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(unpaidInvoices);
        if (!unpaidInvoices.isEmpty()) {
            when(paymentApplicationRepository.save(any(PaymentApplication.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }
    }
}
