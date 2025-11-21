package com.expense_tracker.service;

import com.expense_tracker.exception.ResourceNotFoundException;
import com.expense_tracker.model.Category;
import com.expense_tracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    // create category
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    // get all categories (user + default)
    public List<Category> getCategories(Long userId) {
//        return categoryRepository.findByUserIdOrUserIdIsNull(userId);
        return categoryRepository.findActiveCategories(userId);
    }

    // update category
    @Transactional
    public Category updateCategory(Long id, Category updates) {

        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (updates.getName() != null)
            existing.setName(updates.getName());

        if (updates.getType() != null)
            existing.setType(updates.getType());

        if (updates.getIcon() != null)
            existing.setIcon(updates.getIcon());

        if (updates.getColor() != null)
            existing.setColor(updates.getColor());

        return categoryRepository.save(existing);
    }


    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.isDeleted()) {
            throw new ResourceNotFoundException("Category is already deleted");
        }

        category.setDeleted(true);  // soft delete

        // updatedAt will automatically set if @UpdateTimestamp exists
        categoryRepository.save(category);
    }


}