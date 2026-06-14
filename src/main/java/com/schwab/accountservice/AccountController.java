package com.schwab.accountservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final TransactionRepository transactionRepository;

    public AccountController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<Transaction> addTransaction(
            @PathVariable String accountId,
            @RequestBody TransactionRequest request) {
        
        Transaction transaction = new Transaction(
                accountId, 
                request.amount(), 
                request.type(), 
                Instant.now()
        );
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        return ResponseEntity.ok(savedTransaction);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        
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
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);

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

record TransactionRequest(BigDecimal amount, String type) {}