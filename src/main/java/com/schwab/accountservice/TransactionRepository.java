package com.schwab.accountservice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdOrderByEventTimestampDesc(String accountId);
    Optional<Transaction> findByEventId(String eventId);
}