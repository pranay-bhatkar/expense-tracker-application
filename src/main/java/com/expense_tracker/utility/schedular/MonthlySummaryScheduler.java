package com.expense_tracker.utility.schedular;

import com.expense_tracker.model.User;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonthlySummaryScheduler {

    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 10 1 * *")  // runs on 1st of every month
    public void sendMonthlySummary() {

        LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<User> users = transactionRepository.findDistinctUsers();

        for (User user : users) {
            double income = transactionRepository.sumIncome(user.getId(), start, end);
            double expense = transactionRepository.sumExpense(user.getId(), start, end);
            double savings = income - expense;

            notificationService.sendNotification(
                    user,
                    "Monthly Summary",
                    "📅 Monthly Summary:\n" +
                            "Income: ₹" + income + "\n" +
                            "Expense: ₹" + expense + "\n" +
                            "Savings: ₹" + savings
            );
        }
    }
}