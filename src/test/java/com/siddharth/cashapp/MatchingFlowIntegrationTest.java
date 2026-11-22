package com.siddharth.cashapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

/**
 * Integration test: the whole app (controllers, services, real JPA queries) on the in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatchingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paymentIsMatchedOnlyToItsOwnCustomersInvoices() throws Exception {
        // Step 1: two customers
        long acmeId = createAndReturnId("/customers", """
                {"name": "Acme Ltd", "email": "ap@acme.example"}
                """);
        long globexId = createAndReturnId("/customers", """
                {"name": "Globex", "email": "ap@globex.example"}
                """);

        // Step 2: one invoice each
        createAndReturnId("/invoices", """
                {"invoiceNumber": "IT-ACME-1", "customerId": %d, "totalAmount": 500.00, "dueDate": "2026-01-15"}
                """.formatted(acmeId));
        createAndReturnId("/invoices", """
                {"invoiceNumber": "IT-GLOBEX-1", "customerId": %d, "totalAmount": 500.00, "dueDate": "2026-01-01"}
                """.formatted(globexId));

        // Step 3: Acme pays 700 and (wrongly) names Globex's invoice first
        long paymentId = createAndReturnId("/payments", """
                {"customerId": %d, "amount": 700.00, "paymentDate": "2026-02-01",
                 "referenceText": "IT-GLOBEX-1 IT-ACME-1"}
                """.formatted(acmeId));

        // Step 4: match. Only Acme's invoice is paid; 200 stays unapplied.
        mockMvc.perform(post("/payments/" + paymentId + "/match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications.length()").value(1))
                .andExpect(jsonPath("$.applications[0].invoiceNumber").value("IT-ACME-1"))
                .andExpect(jsonPath("$.applications[0].matchRule").value("REFERENCE_MATCH"))
                .andExpect(jsonPath("$.unappliedAmount").value(200.00))
                .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_APPLIED"));

        // Step 5: Globex's invoice is still fully open
        mockMvc.perform(get("/invoices").param("customerId", String.valueOf(globexId)))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].openAmount").value(500.00));
    }

    @Test
    void duplicateInvoiceNumberReturns409() throws Exception {
        long customerId = createAndReturnId("/customers", """
                {"name": "Initech", "email": "ap@initech.example"}
                """);
        String invoiceBody = """
                {"invoiceNumber": "IT-DUP-1", "customerId": %d, "totalAmount": 100.00, "dueDate": "2026-01-15"}
                """.formatted(customerId);
        createAndReturnId("/invoices", invoiceBody);

        mockMvc.perform(post("/invoices").contentType(MediaType.APPLICATION_JSON).content(invoiceBody))
                .andExpect(status().isConflict());
    }

    // Sends a POST, expects 201 Created, and returns the "id" from the JSON response.
    private long createAndReturnId(String url, String jsonBody) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
