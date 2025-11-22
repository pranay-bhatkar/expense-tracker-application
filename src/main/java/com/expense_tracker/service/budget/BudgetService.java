package com.expense_tracker.service.budget;

import com.expense_tracker.dto.budget.BudgetRequestDTO;
import com.expense_tracker.dto.budget.BudgetResponseDTO;
import com.expense_tracker.exception.AccessDeniedException;
import com.expense_tracker.exception.ResourceNotFoundException;
import com.expense_tracker.model.Category;
import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.User;
import com.expense_tracker.model.budget.Budget;
import com.expense_tracker.repository.CategoryRepository;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.repository.budget.BudgetRepository;
import com.expense_tracker.service.UserService;
import com.expense_tracker.service.notification.NotificationService;
import com.expense_tracker.utility.mapper.BudgetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService; // to get logged-in user
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public BudgetResponseDTO createBudget(BudgetRequestDTO dto) {
        User user = userService.getCurrentUser();

        Long categoryId = dto.getCategoryId() == null ? null : dto.getCategoryId();
        budgetRepository.findByUserIdAndMonthAndYearAndCategoryId(
                user.getId(),
                dto.getMonth(),
                dto.getYear(),
                categoryId
        ).ifPresent(b -> {
            throw new RuntimeException("Budget already exists for this month/year/category");
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

        budgetRepository.delete(budget);
    }

    // Update Spent After Transaction
    public void updateSpentForBudget(Transaction transaction) {
        User user = transaction.getUser();
        int month = transaction.getDate().getMonthValue();
        int year = transaction.getDate().getYear();
        Long categoryId = transaction.getCategory() != null ? transaction.getCategory().getId() : null;

        // update category budget
        budgetRepository.findByUserIdAndMonthAndYearAndCategoryId(
                        user.getId(),
                        month,
                        year,
                        categoryId
                )
                .ifPresent(budget -> {
                    double spent = calculateSpent(categoryId, user.getId(), month, year);
                    budget.setSpent(spent);
                    budgetRepository.save(budget);

                    if (spent > budget.getAmount()) {
                        notificationService.sendNotification(
                                user,
                                "Budget exceeded for " + (budget.getCategory() != null ?
                                        budget.getCategory().getName() : "Overall") + " in " + month + " / " + year
                        );
                    }
                });

        // update overall budget
        budgetRepository.findByUserIdAndMonthAndYearAndCategoryId(
                        user.getId(),
                        month,
                        year,
                        null
                )
                .ifPresent(budget -> {
                    double spent = calculateSpent(null, user.getId(), month, year);
                    budget.setSpent(spent);
                    budgetRepository.save(budget);

                    if (spent > budget.getAmount()) {
                        notificationService.sendNotification(
                                user,
                                "Overall monthly budget exceeded in " + month + "/" + year
                        );
                    }
                });


    }

    /* Helper: calculate spent amount */
    private double calculateSpent(Long categoryId, Long userId, int month, int year) {
        return transactionRepository.sumSpent(userId, month, year, categoryId);

    }


}