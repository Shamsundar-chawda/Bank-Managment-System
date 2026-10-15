package com.example.bankapp.repository;

import com.example.bankapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdOrderByTimestampAsc(Long accountId);

    List<Transaction> findByAccountIdAndTimestampBetweenOrderByTimestampAsc(
            Long accountId, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByAccountIdAndTimestampGreaterThanEqualOrderByTimestampAsc(
            Long accountId, LocalDateTime start);

    List<Transaction> findByAccountIdAndTimestampLessThanEqualOrderByTimestampAsc(
            Long accountId, LocalDateTime end);
}
