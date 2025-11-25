package com.expense_tracker.repository;

import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.TransactionType;
import com.expense_tracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUser_IdAndArchivedFalse(Long userId, Pageable pageable);

    Page<Transaction> findByUser_IdAndTypeAndArchivedFalse(
            Long userId,
            TransactionType type,
            Pageable pageable);

    Page<Transaction> findByUser_IdAndCategory_IdAndArchivedFalse(
            Long userId,
            Long categoryId,
            Pageable pageable);

    Page<Transaction> findByUser_IdAndDateBetweenAndArchivedFalse(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user.id = :userId
               AND FUNCTION('MONTH', t.date) = :month
               AND FUNCTION('YEAR', t.date) = :year
              AND t.archived = false
              AND t.type = 'EXPENSE'
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
            """)
    double sumSpent(Long userId, Integer month, Integer year, Long categoryId);


    @Query("SELECT t.category.name, COALESCE(SUM(t.amount), 0) " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND FUNCTION('MONTH', t.date) = :month " +
            "AND FUNCTION('YEAR', t.date) = :year " +
            "GROUP BY t.category.name")
    List<Object[]> getCategoryWiseSpending(@Param("userId") Long userId,
                                           @Param("month") int month,
                                           @Param("year") int year);


    // Monthly trend for last N months
    // TransactionRepository.java
    @Query(value = """
            SELECT YEAR(t.date) AS yr,
                   MONTH(t.date) AS mn,
                   SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END) AS income,
                   SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END) AS expense
            FROM transactions t
            WHERE t.user_id = :userId
              AND t.date >= :startDate
              AND t.archived = false
            GROUP BY YEAR(t.date), MONTH(t.date)
            ORDER BY YEAR(t.date), MONTH(t.date)
            """, nativeQuery = true)
    List<Object[]> getMonthlyTrendsNative(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );


    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND FUNCTION('MONTH', t.date) = :month " +
            "AND FUNCTION('YEAR', t.date) = :year " +
            "AND t.type = :type")
    Double sumByUserAndMonthAndType(@Param("userId") Long userId,
                                    @Param("month") int month,
                                    @Param("year") int year,
                                    @Param("type") TransactionType type);



    @Query("SELECT t FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND FUNCTION('MONTH', t.date) = :month " +
            "AND FUNCTION('YEAR', t.date) = :year ")
    List<Transaction> findByUserIdAndMonthAndYear(
            @Param("userId") Long userId,
            @Param("month") int month,
            @Param("year") int year
    );


    List<Transaction> findByRecurringDate(LocalDate tomorrow);

    List<User> findDistinctUsers();

    double sumIncome(Long id, LocalDate start, LocalDate end);

    double sumExpense(Long id, LocalDate start, LocalDate end);
}