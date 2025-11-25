package com.expense_tracker.service.admin;

import com.expense_tracker.dto.admin.AdminDashboardDTO;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserService userService;
    private final TransactionRepository transactionRepository;

    @Cacheable(value = "adminDashboard", key = "#root.methodName")
    public AdminDashboardDTO getDashboardStats() {

        long totalUsers = userService.countAllUsers();
        long activeUsers = userService.countActiveUsers();
        long totalTransactions = transactionRepository.count();
        double totalIncome = transactionRepository.sumAllIncome();
        double totalExpense = transactionRepository.sumAllExpenses();
        long recurringPayments = transactionRepository.countRecurringTransactions();

        return AdminDashboardDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalTransactions(totalTransactions)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .recurringPayments(recurringPayments)
                .build();
    }


    @CacheEvict(value = "adminDashboard", allEntries = true)
    public void clearDashboardCache() {
        // This clears the cached dashboard data
    }


}