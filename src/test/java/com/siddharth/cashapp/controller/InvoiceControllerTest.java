package com.siddharth.cashapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.siddharth.cashapp.exception.ResourceNotFoundException;
import com.siddharth.cashapp.service.InvoiceService;

/**
 * @WebMvcTest starts only the web layer (InvoiceController + GlobalExceptionHandler),
 * no database. MockMvc sends fake HTTP requests without opening a real port.
 */
@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Replaces the real InvoiceService bean with a Mockito mock.
    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    void invalidInvoiceReturns400WithFieldErrors() throws Exception {
        String invalidBody = """
                {
                  "invoiceNumber": "",
                  "totalAmount": -5
                }
                """;

        mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.invoiceNumber").value("invoiceNumber is required"))
                .andExpect(jsonPath("$.fieldErrors.customerId").value("customerId is required"))
                .andExpect(jsonPath("$.fieldErrors.totalAmount").value("totalAmount must be greater than zero"))
                .andExpect(jsonPath("$.fieldErrors.dueDate").value("dueDate is required"));
    }

    @Test
    void missingInvoiceReturns404() throws Exception {
        when(invoiceService.getInvoice(999L)).thenThrow(new ResourceNotFoundException("Invoice 999 not found"));

        mockMvc.perform(get("/invoices/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Invoice 999 not found"));
    }
}
