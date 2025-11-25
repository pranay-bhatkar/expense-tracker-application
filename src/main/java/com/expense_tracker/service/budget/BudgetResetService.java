package com.expense_tracker.service.budget;

import com.expense_tracker.model.budget.Budget;
import com.expense_tracker.model.budget.BudgetHistory;
import com.expense_tracker.repository.budget.BudgetHistoryRepository;
import com.expense_tracker.repository.budget.BudgetRepository;
import com.expense_tracker.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetResetService {

    private final BudgetRepository budgetRepository;
    private final BudgetHistoryRepository budgetHistoryRepository;
    private final NotificationService notificationService;

    // Runs every month at midnight on 1st day
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthlyBudgets() {
        LocalDate now = LocalDate.now();
        int previousMonth = now.minusMonths(1).getMonthValue();
        int previousYear = now.minusMonths(1).getYear();

        // Fetch only budgets from previous month
        List<Budget> budgetsToReset = budgetRepository.findByMonthAndYear(previousMonth, previousYear);

        // Archive previous month budgets
        List<BudgetHistory> historyList = budgetsToReset.stream()
                .map(budget -> BudgetHistory.builder()
                        .originalBudgetId(budget.getId())
                        .amount(budget.getAmount())
                        .spent(budget.getSpent())
                        .month(budget.getMonth())
                        .year(budget.getYear())
                        .category(budget.getCategory())
                        .user(budget.getUser())
                        .archivedAt(LocalDateTime.now())
                        .build()
                ).toList();

        budgetHistoryRepository.saveAll(historyList);

        // Reset budgets for new month
        for (Budget budget : budgetsToReset) {
            budget.setSpent(0.0);
            budget.setMonth(now.getMonthValue());
            budget.setYear(now.getYear());

            // Send new month budget notification to each user
            String message = "Your new month budget is ready for " + now.getMonth() + " " + now.getYear();
            notificationService.sendNotification(budget.getUser(),"Monthly budget reset", message);
        }

        budgetRepository.saveAll(budgetsToReset);

        System.out.println("Monthly budgets reset successfully for " +
                now.getMonth() + " " + now.getYear());
    }
}


/*
 * Explanation of Cron Expression
 * Field	Value	Meaning
 * Seconds	0	At 0 seconds
 * Minutes	0	At 0 minutes
 * Hours	0	At 0 hour (midnight)
 * Day of month	1	On the 1st day of month
 * Month	*	Every month
 * Day of week	*	Any day of the week
 *
 * Effect:

 * This runs once every month, at midnight on the 1st day.
 */