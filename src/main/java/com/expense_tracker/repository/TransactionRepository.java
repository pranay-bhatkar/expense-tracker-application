package com.expense_tracker.repository;

import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserIdAndArchivedFalse(Long userId, Pageable pageable);

    Page<Transaction> findByUserIdAndTypeAndArchivedFalse(Long userId, TransactionType type, Pageable pageable);

    Page<Transaction> findByUserIdAndCategoryIdAndArchivedFalse(Long userId, Long categoryId, Pageable pageable);

    Page<Transaction> findByUserIdAndDateBetweenAndArchivedFalse(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);
}