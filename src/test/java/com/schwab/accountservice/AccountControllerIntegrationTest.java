package com.schwab.accountservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    void testAddTransaction_Success() throws Exception {
        String accountId = "acct-1";
        TransactionRequest request = new TransactionRequest("evt-1", new BigDecimal("100.00"), "CREDIT", Instant.now());

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1"));
    }

    @Test
    void testAddTransaction_Idempotency() throws Exception {
        String accountId = "acct-2";
        String eventId = "evt-2";
        TransactionRequest request = new TransactionRequest(eventId, new BigDecimal("100.00"), "CREDIT", Instant.now());

        // First request
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second request with the same eventId
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Returns 200 OK for duplicates
                .andExpect(jsonPath("$.eventId").value(eventId));
    }

    @Test
    void testGetBalance_CorrectCalculation() throws Exception {
        String accountId = "acct-3";
        
        // Add a credit
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TransactionRequest("evt-3", new BigDecimal("200.00"), "CREDIT", Instant.now()))));

        // Add a debit
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TransactionRequest("evt-4", new BigDecimal("50.00"), "DEBIT", Instant.now()))));

        // Check balance
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void testBalance_OutOfOrderEvents() throws Exception {
        String accountId = "acct-4";
        Instant now = Instant.now();

        // Process a later event first
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TransactionRequest("evt-6", new BigDecimal("50.00"), "DEBIT", now))));

        // Process an earlier event second
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TransactionRequest("evt-5", new BigDecimal("200.00"), "CREDIT", now.minus(1, ChronoUnit.HOURS)))));

        // The final balance should be correct regardless of processing order
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }
}