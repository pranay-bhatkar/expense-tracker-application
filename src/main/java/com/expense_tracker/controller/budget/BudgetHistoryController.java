package com.expense_tracker.controller.budget;

import com.expense_tracker.model.budget.BudgetHistory;
import com.expense_tracker.repository.budget.BudgetHistoryRepository;
import com.expense_tracker.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/budgets/history")
@RequiredArgsConstructor
public class BudgetHistoryController {

    private final BudgetHistoryRepository historyRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetHistory>>> getUserHistory() {
        Long userId = 1L; // fetch from logged-in user
        List<BudgetHistory> history = historyRepository.findByUserIdOrderByYearDescMonthDesc(userId);

        ApiResponse<List<BudgetHistory>> response = new ApiResponse<>(
                "success",
                "History fetched successfully",
                history,
                HttpStatus.CREATED.value()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}