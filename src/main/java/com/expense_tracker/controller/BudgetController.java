package com.expense_tracker.controller;

import com.expense_tracker.dto.budget.BudgetRequestDTO;
import com.expense_tracker.dto.budget.BudgetResponseDTO;
import com.expense_tracker.response.ApiResponse;
import com.expense_tracker.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponseDTO>> createBudget(@RequestBody BudgetRequestDTO dto) {
        BudgetResponseDTO created = budgetService.createBudget(dto);
        ApiResponse<BudgetResponseDTO> response = new ApiResponse<>(
                "success",
                "Budget created",
                created,
                HttpStatus.CREATED.value()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponseDTO>>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        List<BudgetResponseDTO> budgets = budgetService.getBudgets(month, year);
        ApiResponse<List<BudgetResponseDTO>> response = new ApiResponse<>(
                "success",
                "Budgets retrieved",
                budgets,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponseDTO>> updateBudget(
            @PathVariable Long id,
            @RequestBody BudgetRequestDTO dto
    ) {
        BudgetResponseDTO updated = budgetService.updateBudget(id, dto);
        ApiResponse<BudgetResponseDTO> response = new ApiResponse<>(
                "success",
                "Budget updated",
                updated,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        ApiResponse<Void> response = new ApiResponse<>(
                "success",
                "Budget deleted",
                null,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }
}