package com.schwab.accountservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final TransactionRepository transactionRepository;

    public AccountController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/{accountId}/transactions")
    @Transactional
    public ResponseEntity<?> addTransaction(
            @PathVariable String accountId,
            @RequestBody TransactionRequest request) {

        // Idempotency check
        Optional<Transaction> existingTransaction = transactionRepository.findByEventId(request.eventId());
        if (existingTransaction.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(existingTransaction.get());
        }

        // Handle out-of-order events
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByEventTimestampDesc(accountId);
        for (Transaction t : transactions) {
            if (t.getEventTimestamp().isAfter(request.eventTimestamp())) {
                // This is an older event, but we are processing it now.
                // The balance will be correct because it's calculated on the fly.
            }
        }

        Transaction transaction = new Transaction(
                accountId,
                request.amount(),
                request.type(),
                request.eventTimestamp(),
                request.eventId()
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTransaction);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByEventTimestampDesc(accountId);

        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if ("CREDIT".equalsIgnoreCase(transaction.getType())) {
                balance = balance.add(transaction.getAmount());
            } else if ("DEBIT".equalsIgnoreCase(transaction.getType())) {
                balance = balance.subtract(transaction.getAmount());
            }
        }

        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", balance
        ));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Map<String, Object>> getAccountDetails(@PathVariable String accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByEventTimestampDesc(accountId);

        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if ("CREDIT".equalsIgnoreCase(transaction.getType())) {
                balance = balance.add(transaction.getAmount());
            } else if ("DEBIT".equalsIgnoreCase(transaction.getType())) {
                balance = balance.subtract(transaction.getAmount());
            }
        }

        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "balance", balance,
                "transactions", transactions
        ));
    }
}

record TransactionRequest(String eventId, BigDecimal amount, String type, Instant eventTimestamp) {}