package com.expense_tracker.service;

import com.expense_tracker.exception.ResourceNotFoundException;
import com.expense_tracker.model.Category;
import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.TransactionType;
import com.expense_tracker.repository.CategoryRepository;
import com.expense_tracker.repository.TransactionRepository;
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

    public Transaction addTransaction(Transaction transaction, Long userId) {
        Category category = categoryRepository.findById(
                        transaction.getCategory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        transaction.setUserId(userId);
        transaction.setCategory(category);
        return transactionRepository.save(transaction);
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

        return transactionRepository.save(existing);
    }


    public void softDelete(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        t.setArchived(true);
        transactionRepository.save(t);
    }


    public Page<Transaction> getAll(Long userId, int page, int size) {
        return transactionRepository.findByUserIdAndArchivedFalse(userId, PageRequest.of(page, size));
    }

    public Page<Transaction> filterByType(Long userId, TransactionType type, int page, int size) {
        return transactionRepository.findByUserIdAndTypeAndArchivedFalse(userId, type, PageRequest.of(page, size));
    }

    public Page<Transaction> filterByCategory(Long userId, Long categoryId, int page, int size) {
        return transactionRepository.findByUserIdAndCategoryIdAndArchivedFalse(userId, categoryId, PageRequest.of(page, size));
    }

    public Page<Transaction> filterByDateRange(Long userId, LocalDate start, LocalDate end, int page, int size) {
        return transactionRepository.findByUserIdAndDateBetweenAndArchivedFalse(userId, start, end, PageRequest.of(page, size));
    }

    public Transaction attachReceipt(MultipartFile file, Long transactionId, Long userId) throws IOException {

        // 1. Load transaction
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 2. Security check — prevent updating other users' transactions
        if (!tx.getUserId().equals(userId)) {
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