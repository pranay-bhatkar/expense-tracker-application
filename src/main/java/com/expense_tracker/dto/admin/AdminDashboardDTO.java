package com.expense_tracker.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardDTO {
    private static final long serialVersionUID = 1L;

    private long totalUsers;
    private long activeUsers;
    private long totalTransactions;
    private double totalIncome;
    private double totalExpense;
    private long recurringPayments;
}