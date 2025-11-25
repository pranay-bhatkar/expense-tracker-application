package com.expense_tracker.utility.schedular;


import com.expense_tracker.model.Transaction;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler {

    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")   // daily @ 9AM
    public void sendRecurringPaymentReminder() {

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Transaction> upcoming = transactionRepository
                .findByRecurringDate(tomorrow);

        for (Transaction tx : upcoming) {
            notificationService.sendNotification(
                    tx.getUser(),
                    "Recurring Payment Remainder",
                    "💳 Reminder: Recurring payment of ₹"
                            + tx.getAmount() + " is due tomorrow."
            );
        }
    }
}