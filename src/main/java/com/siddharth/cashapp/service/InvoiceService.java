package com.siddharth.cashapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siddharth.cashapp.dto.CreateInvoiceRequest;
import com.siddharth.cashapp.dto.InvoiceResponse;
import com.siddharth.cashapp.exception.BusinessRuleException;
import com.siddharth.cashapp.exception.ResourceNotFoundException;
import com.siddharth.cashapp.model.Customer;
import com.siddharth.cashapp.model.Invoice;
import com.siddharth.cashapp.model.InvoiceStatus;
import com.siddharth.cashapp.repository.InvoiceRepository;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerService customerService;

    public InvoiceService(InvoiceRepository invoiceRepository, CustomerService customerService) {
        this.invoiceRepository = invoiceRepository;
        this.customerService = customerService;
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        if (invoiceRepository.existsByInvoiceNumber(request.invoiceNumber())) {
            throw new BusinessRuleException("Invoice number " + request.invoiceNumber() + " already exists");
        }

        Customer customer = customerService.findCustomerOrThrow(request.customerId());
        Invoice invoice = new Invoice(request.invoiceNumber(), customer, request.totalAmount(), request.dueDate());
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return DtoMapper.toInvoiceResponse(savedInvoice);
    }

    /**
     * Both filters are optional: null means "don't filter on this".
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> searchInvoices(Long customerId, InvoiceStatus status) {
        List<Invoice> invoices;
        if (customerId != null && status != null) {
            invoices = invoiceRepository.findByCustomerIdAndStatusOrderByDueDateAsc(customerId, status);
        } else if (customerId != null) {
            invoices = invoiceRepository.findByCustomerIdOrderByDueDateAsc(customerId);
        } else if (status != null) {
            invoices = invoiceRepository.findByStatusOrderByDueDateAsc(status);
        } else {
            invoices = invoiceRepository.findAll();
        }

        List<InvoiceResponse> responses = new ArrayList<>();
        for (Invoice invoice : invoices) {
            responses.add(DtoMapper.toInvoiceResponse(invoice));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice " + invoiceId + " not found"));
        return DtoMapper.toInvoiceResponse(invoice);
    }
}
