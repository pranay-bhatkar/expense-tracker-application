package com.expense_tracker.service.budget;

import com.expense_tracker.dto.budget.BudgetRequestDTO;
import com.expense_tracker.dto.budget.BudgetResponseDTO;
import com.expense_tracker.exception.AccessDeniedException;
import com.expense_tracker.exception.ResourceNotFoundException;
import com.expense_tracker.model.Category;
import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.User;
import com.expense_tracker.model.budget.Budget;
import com.expense_tracker.model.budget.BudgetHistory;
import com.expense_tracker.model.budget.BudgetHistoryType;
import com.expense_tracker.repository.CategoryRepository;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.repository.UserRepository;
import com.expense_tracker.repository.budget.BudgetHistoryRepository;
import com.expense_tracker.repository.budget.BudgetRepository;
import com.expense_tracker.service.UserService;
import com.expense_tracker.service.notification.NotificationService;
import com.expense_tracker.utility.mapper.BudgetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService; // to get logged-in user
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final BudgetHistoryRepository budgetHistoryRepository;


    public BudgetResponseDTO createBudget(BudgetRequestDTO dto) {
        log.info("Enter in the createBudget service");
        User user = userService.getCurrentUser();

        Long categoryId = dto.getCategoryId() == null ? null : dto.getCategoryId();

        String categoryName;
        if (dto.getCategoryId() != null) {
            categoryName = categoryRepository.findById(dto.getCategoryId())
                    .map(Category::getName)
                    .orElse("Unknown Category");
        } else {
            categoryName = "General";
        }

        budgetRepository.findByUserIdAndMonthAndYearAndCategoryId(
                user.getId(),
                dto.getMonth(),
                dto.getYear(),
                categoryId
        ).ifPresent(b -> {
            throw new ResourceNotFoundException("Budget already exists for this : " + categoryName + ", " + "Month : " +
                    dto.getMonth() + ", " +
                    "Year : " +
                    dto.getYear()
            );
        });

        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Budget budget = Budget.builder()
                .amount(dto.getAmount())
                .month(dto.getMonth())
                .year(dto.getYear())
                .category(category)
                .user(user)
                .spent(0.0)
                .build();

        budgetRepository.save(budget);
        saveHistory(budget, BudgetHistoryType.CREATED);
        return BudgetMapper.toDTO(budget);

    }

    public BudgetResponseDTO updateBudget(Long id, BudgetRequestDTO dto) {
        User user = userService.getCurrentUser();

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized");
        }

        budget.setAmount(dto.getAmount());
        budgetRepository.save(budget);

        saveHistory(budget, BudgetHistoryType.UPDATED);
        return BudgetMapper.toDTO(budget);
    }

    public List<BudgetResponseDTO> getBudgets(Integer month, Integer year) {
        User user = userService.getCurrentUser();
        List<Budget> budgets;

        if (month != null && year != null) {
            budgets = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
        } else {
            budgets = budgetRepository.findByUserId(user.getId());
        }


        return budgets.stream()
                .map(BudgetMapper::toDTO)
                .toList();
    }

    public void deleteBudget(Long id) {
        User user = userService.getCurrentUser();

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        saveHistory(budget, BudgetHistoryType.DELETED);
        budgetRepository.delete(budget);
    }

    // Update Spent After Transaction
    public void updateSpentForBudget(Transaction transaction) {

        User user = transaction.getUser();
        int month = transaction.getDate().getMonthValue();
        int year = transaction.getDate().getYear();
        Long categoryId = transaction.getCategory() != null ? transaction.getCategory().getId() : null;

        // 1️⃣ Update Category Budget (categoryId != null)
        budgetRepository.findByUserIdAndMonthAndYearAndCategoryId(user.getId(), month, year, categoryId)
                .ifPresent(budget -> {
                    double spent = calculateSpent(categoryId, user.getId(), month, year);
                    budget.setSpent(spent);
                    budgetRepository.save(budget);

                    handleBudgetNotifications(user, budget, spent, month, year);
                });

        // 2️⃣ Update Overall Budget (categoryId == null)
        budgetRepository.findByUserIdAndMonthAndYearAndCategoryId(user.getId(), month, year, null)
                .ifPresent(budget -> {
                    double spent = calculateSpent(null, user.getId(), month, year);
                    budget.setSpent(spent);
                    budgetRepository.save(budget);

                    handleBudgetNotifications(user, budget, spent, month, year);
                });
    }

    /* Helper: calculate spent amount */
    private double calculateSpent(Long categoryId, Long userId, int month, int year) {
        return transactionRepository.sumSpent(userId, month, year, categoryId);

    }

    // reset the budget
    public void resetBudgetsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Budget> budgets = budgetRepository.findByUserId(user.getId());

        for (Budget budget : budgets) {
            budget.setSpent(0.0); // reset spent
            budgetRepository.save(budget);

            saveHistory(budget, BudgetHistoryType.RESET);
        }
    }


    // helper methods
    private void saveHistory(Budget budget, BudgetHistoryType action) {

        BudgetHistory history = BudgetHistory.builder()
                .originalBudgetId(budget.getId())
                .amount(budget.getAmount())
                .spent(budget.getSpent())
                .month(budget.getMonth())
                .year(budget.getYear())
                .category(budget.getCategory())
                .user(budget.getUser())
                .type(action)
                .archivedAt(LocalDateTime.now()) // snapshot time
                .build();

        budgetHistoryRepository.save(history);
    }


    // -----------------------------------------------
    // HELPER FOR CHANGE TYPE
    // -----------------------------------------------
//    private String getChangeType(Double oldAmount, Double newAmount) {
//        if (oldAmount == null) return "CREATED";
//        if (newAmount > oldAmount) return "INCREASE";
//        if (newAmount < oldAmount) return "DECREASE";
//        return "NO CHANGE";
//    }


    // handle notifications
    private void handleBudgetNotifications(User user, Budget budget, double spent, int month, int year) {

        double limit = budget.getAmount();
        String categoryName =
                (budget.getCategory() != null ? budget.getCategory().getName() : "Overall Budget");

        // 💥 1️⃣ Budget Exceeded
        if (spent >= limit) {
            notificationService.sendNotification(
                    user,
                    "Budget Exceeded 🚨",
                    "You exceeded the budget for " + categoryName + " in " + month + "/" + year
            );
            return;
        }

        // ⚠ 2️⃣ 80% Warning
        if (spent >= limit * 0.8) {
            notificationService.sendNotification(
                    user,
                    "Budget Warning ⚠",
                    "You have reached 80% of your " + categoryName + " budget."
            );
        }
    }

}