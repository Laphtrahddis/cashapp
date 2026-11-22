package com.siddharth.cashapp.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.siddharth.cashapp.model.Invoice;
import com.siddharth.cashapp.model.InvoiceStatus;

/**
 * Spring Data builds the SQL from each method name.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByCustomerIdOrderByDueDateAsc(Long customerId);

    List<Invoice> findByStatusOrderByDueDateAsc(InvoiceStatus status);

    List<Invoice> findByCustomerIdAndStatusOrderByDueDateAsc(Long customerId, InvoiceStatus status);

    // Used by matching: one customer's unpaid invoices, oldest due date first.
    // Generated SQL: WHERE customer_id = ? AND status IN (?, ?) ORDER BY due_date ASC, id ASC
    List<Invoice> findByCustomerIdAndStatusInOrderByDueDateAscIdAsc(Long customerId,
                                                                    Collection<InvoiceStatus> statuses);
}
