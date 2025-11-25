package com.expense_tracker.service.transaction;

import com.expense_tracker.dto.transaction.TransactionRequestDTO;
import com.expense_tracker.exception.ResourceNotFoundException;
import com.expense_tracker.model.Category;
import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.TransactionType;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.CategoryRepository;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.service.UserService;
import com.expense_tracker.service.admin.AdminService;
import com.expense_tracker.service.budget.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final BudgetService budgetService;
    private final AdminService adminService;

    public Transaction addTransaction(TransactionRequestDTO dto, Long userId) {
        User user = userService.getUserById(userId);

        Category category = categoryRepository.findById(
                        dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Transaction transaction = Transaction.builder()
                .amount(dto.getAmount())
                .type(TransactionType.valueOf(dto.getType()))
                .notes(dto.getNotes())
                .recurring(dto.getRecurring() != null && dto.getRecurring()) // FIX HERE
                .date(dto.getDate())
                .user(user)
                .category(category)
                .build();

        Transaction saved = transactionRepository.save(transaction);


        // Update budget after creating transaction
        budgetService.updateSpentForBudget(saved);

        // Clear Admin Dashboard cache so stats are refreshed
        adminService.clearDashboardCache();

        return saved;
    }

    // full update
//    public Transaction updateTransaction(Long id, Transaction updated) {
//        Transaction t = transactionRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
//
//        t.setAmount(updated.getAmount());
//        t.setType(updated.getType());
//        t.setNotes(updated.getNotes());
//        t.setDate(updated.getDate());
//        t.setRecurring(updated.isRecurring());
//        t.setCategory(updated.getCategory());
//
//        return transactionRepository.save(t);
//    }

    public Transaction updateTransaction(Long id, Transaction incoming) {

        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Store snapshot to recalc old budget
        Transaction oldSnapshot = new Transaction();
        oldSnapshot.setUser(existing.getUser());
        oldSnapshot.setDate(existing.getDate());
        oldSnapshot.setCategory(existing.getCategory());
        oldSnapshot.setAmount(existing.getAmount());
        oldSnapshot.setType(existing.getType());

        // Only update non-null fields
        if (incoming.getType() != null)
            existing.setType(incoming.getType());

        if (incoming.getAmount() != null)
            existing.setAmount(incoming.getAmount());

        if (incoming.getNotes() != null)
            existing.setNotes(incoming.getNotes());

        if (incoming.getDate() != null)
            existing.setDate(incoming.getDate());

        if (incoming.getCategory() != null && incoming.getCategory().getId() != null) {
            Category category = categoryRepository.findById(incoming.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            existing.setCategory(category);
        }

        if (incoming.getReceiptPath() != null)
            existing.setReceiptPath(incoming.getReceiptPath());

        // recurring
        existing.setRecurring(incoming.isRecurring());

        // archived
        existing.setArchived(incoming.isArchived());

        Transaction saved = transactionRepository.save(existing);

        // 🚀 Update budgets (old and new)
        budgetService.updateSpentForBudget(oldSnapshot);
        budgetService.updateSpentForBudget(saved);

        adminService.clearDashboardCache(); // clear cache

        return saved;
    }


    public void softDelete(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        t.setArchived(true);
        transactionRepository.save(t);

        // 🚀 Recalculate budgets after deletion
        budgetService.updateSpentForBudget(t);
        adminService.clearDashboardCache(); // clear cache

    }

    public void restore(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        t.setArchived(false);
        transactionRepository.save(t);

        // 🚀 Recalculate budgets after restore
        budgetService.updateSpentForBudget(t);
    }

    public Page<Transaction> getAll(Long userId, int page, int size) {
        return transactionRepository.findByUser_IdAndArchivedFalse(userId, PageRequest.of(page, size));
    }

    public Page<Transaction> filterByType(Long userId, TransactionType type, int page, int size) {
        return transactionRepository.findByUser_IdAndTypeAndArchivedFalse(userId, type, PageRequest.of(page, size));
    }

    public Page<Transaction> filterByCategory(Long userId, Long categoryId, int page, int size) {
        return transactionRepository.findByUser_IdAndCategory_IdAndArchivedFalse(userId, categoryId,
                PageRequest.of(page, size));
    }

    public Page<Transaction> filterByDateRange(Long userId, LocalDate start, LocalDate end, int page, int size) {
        return transactionRepository.findByUser_IdAndDateBetweenAndArchivedFalse(userId, start, end,
                PageRequest.of(page, size));
    }

    public Transaction attachReceipt(MultipartFile file, Long transactionId, Long userId) throws IOException {

        // 1. Load transaction
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 2. Security check — prevent updating other users' transactions
        if (!tx.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized – cannot modify this transaction");
        }

        // 3. Create uploads folder
        Path uploadPath = Paths.get("uploads/");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 4. Save file
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath);

        // 5. Update DB
        tx.setReceiptPath(fileName);

        return transactionRepository.save(tx);
    }

}