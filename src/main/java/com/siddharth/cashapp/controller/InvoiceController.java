package com.siddharth.cashapp.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.siddharth.cashapp.dto.CreateInvoiceRequest;
import com.siddharth.cashapp.dto.InvoiceResponse;
import com.siddharth.cashapp.model.InvoiceStatus;
import com.siddharth.cashapp.service.InvoiceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse createdInvoice = invoiceService.createInvoice(request);
        URI location = URI.create("/invoices/" + createdInvoice.id());
        return ResponseEntity.created(location).body(createdInvoice);
    }

    // Examples: GET /invoices    GET /invoices?customerId=1    GET /invoices?customerId=1&status=OPEN
    @GetMapping
    public List<InvoiceResponse> searchInvoices(@RequestParam(required = false) Long customerId,
                                                @RequestParam(required = false) InvoiceStatus status) {
        return invoiceService.searchInvoices(customerId, status);
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable Long invoiceId) {
        return invoiceService.getInvoice(invoiceId);
    }
}
