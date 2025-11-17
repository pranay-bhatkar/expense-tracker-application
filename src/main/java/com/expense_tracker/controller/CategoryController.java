package com.expense_tracker.controller;

import com.expense_tracker.exception.UserNotFoundException;
import com.expense_tracker.model.Category;
import com.expense_tracker.repository.CategoryRepository;
import com.expense_tracker.repository.UserRepository;
import com.expense_tracker.response.ApiResponse;
import com.expense_tracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @RequestBody Category category,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        Long userId = null;
        if (userDetails != null) {
            userId = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found"))
                    .getId();
        }

        category.setUserId(userId);

        // Check for duplicates
        boolean exists = categoryRepository.existsByNameAndUserId(category.getName(), userId);
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(
                            "error",
                            "Category with this name already exists",
                            null,
                            HttpStatus.CONFLICT.value())
                    );
        }

        Category saved = categoryService.createCategory(category);
        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Category created",
                saved,
                HttpStatus.OK.value()
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = null;
        if (userDetails != null) {
            userId = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found"))
                    .getId();
        }

        List<Category> categories = categoryService.getCategories(userId);

        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Categories fetched successfully",
                categories,
                HttpStatus.OK.value()
        ));
    }


    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody Category category
    ) {
        Category updated = categoryService.updateCategory(id, category);

        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Category updated successfully",
                updated,
                HttpStatus.OK.value()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id
    ) {
        categoryService.deleteCategory(id);


        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Category deleted successfully",
                null,
                HttpStatus.OK.value()
        ));
    }
}