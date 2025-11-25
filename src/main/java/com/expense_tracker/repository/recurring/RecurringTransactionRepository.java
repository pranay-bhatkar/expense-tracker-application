package com.expense_tracker.repository.recurring;

import com.expense_tracker.model.recurring.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByNextExecutionDateLessThanEqual(LocalDate date);

    @Query("SELECT r FROM RecurringTransaction r WHERE r.nextExecutionDate = :tomorrow")
    List<RecurringTransaction> findDueTomorrow(@Param("tomorrow") LocalDate tomorrow);
}