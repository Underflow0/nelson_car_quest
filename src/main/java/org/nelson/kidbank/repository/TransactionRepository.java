package org.nelson.kidbank.repository;

import org.nelson.kidbank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    /** Used for idempotency check: find existing INTEREST tx for an account + month. */
    Optional<Transaction> findByAccountIdAndTypeAndYearMonth(
            Long accountId, Transaction.Type type, String yearMonth);
}
