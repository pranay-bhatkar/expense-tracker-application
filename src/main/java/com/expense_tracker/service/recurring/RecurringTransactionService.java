package com.expense_tracker.service.recurring;

import com.expense_tracker.dto.recurring.RecurringTransactionRequestDTO;
import com.expense_tracker.model.Category;
import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.TransactionType;
import com.expense_tracker.model.User;
import com.expense_tracker.model.recurring.RecurringTransaction;
import com.expense_tracker.repository.CategoryRepository;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.repository.UserRepository;
import com.expense_tracker.repository.recurring.RecurringTransactionRepository;
import com.expense_tracker.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // create recurring transaction
    public RecurringTransaction addRecurring(RecurringTransactionRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        RecurringTransaction recurring = RecurringTransaction.builder()
                .amount(dto.getAmount())
                .frequency(dto.getFrequency())
                .nextExecutionDate(dto.getNextExecutionDate())
                .user(user)
                .category(category)
                .build();

        return recurringTransactionRepository.save(recurring);
    }


    public List<RecurringTransaction> getAllRecurring() {
        return recurringTransactionRepository.findAll();
    }


    public void deleteRecurring(Long id) {
        recurringTransactionRepository.deleteById(id);
    }


    // CRON job: runs every day at 00:00
    @Scheduled(cron = "0 0 0 * * *")
    public void executeRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> dueTransactions = recurringTransactionRepository.findByNextExecutionDateLessThanEqual(today);

        for (RecurringTransaction recurring : dueTransactions) {
            // Create a new transaction
            Transaction transaction = Transaction.builder()
                    .amount(recurring.getAmount())
                    .category(recurring.getCategory())
                    .user(recurring.getUser())
                    .type(TransactionType.EXPENSE) // assuming recurring expenses; can extend for INCOME
                    .date(today)
                    .build();

            transactionRepository.save(transaction);

            // Send email + in-app notification
            String message = "Recurring transaction of amount " + recurring.getAmount() +
                    " for " + (recurring.getCategory() != null ? recurring.getCategory().getName() : "General") +
                    " has been recorded today.";
            notificationService.sendNotification(recurring.getUser(), "Recurring Payment Processed ✔", message);

            // Update next execution date
            switch (recurring.getFrequency()) {
                case WEEKLY ->
                        recurring.setNextExecutionDate(recurring.getNextExecutionDate().plus(1, ChronoUnit.WEEKS));
                case MONTHLY ->
                        recurring.setNextExecutionDate(recurring.getNextExecutionDate().plus(1, ChronoUnit.MONTHS));
                case YEARLY ->
                        recurring.setNextExecutionDate(recurring.getNextExecutionDate().plus(1, ChronoUnit.YEARS));
            }

            recurringTransactionRepository.save(recurring);
        }
    }

    public RecurringTransaction getById(Long id) {
        return recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
    }


}