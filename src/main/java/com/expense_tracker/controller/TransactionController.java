package com.expense_tracker.controller;

import com.expense_tracker.exception.UserNotFoundException;
import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.TransactionType;
import com.expense_tracker.repository.UserRepository;
import com.expense_tracker.response.ApiResponse;
import com.expense_tracker.service.TransactionService;
import com.expense_tracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> addTransaction(
            @RequestBody Transaction transaction,
            @AuthenticationPrincipal UserDetails userDetails) {

//        throws error ->   "data": "For input string: \"test@1.com\"",
//        Long userId = Long.valueOf(userDetails.getUsername());

        Long userId = null;
        if (userDetails != null) {
            userId = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found"))
                    .getId();
        }

        Transaction saved = transactionService.addTransaction(transaction, userId);

        return ResponseEntity.ok(new ApiResponse<>("success", "Transaction added", saved, 200));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> updateTransaction(
            @PathVariable Long id,
            @RequestBody Transaction transaction) {

        Transaction updated = transactionService.updateTransaction(id, transaction);

        return ResponseEntity.ok(new ApiResponse<>("success", "Transaction updated", updated, 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> archiveTransaction(@PathVariable Long id) {
        transactionService.softDelete(id);
        return ResponseEntity.ok(new ApiResponse<>("success", "Transaction archived", null, 200));
    }

    @GetMapping
    public Page<Transaction> getAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int page,
            @RequestParam int size) {

//        Long userId = Long.valueOf(userDetails.getUsername());

        Long userId = null;
        if (userDetails != null) {
            userId = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found"))
                    .getId();
        }

        return transactionService.getAll(userId, page, size);
    }

    @GetMapping("/filter/type")
    public Page<Transaction> filterByType(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam TransactionType type,
            @RequestParam int page,
            @RequestParam int size) {

//        Long userId = Long.valueOf(userDetails.getUsername());

        Long userId = null;
        if (userDetails != null) {
            userId = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found"))
                    .getId();
        }

        return transactionService.filterByType(userId, type, page, size);
    }

    @GetMapping("/filter/date")
    public Page<Transaction> filterByDate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam int page,
            @RequestParam int size) {

//        Long userId = Long.valueOf(userDetails.getUsername());

        Long userId = null;
        if (userDetails != null) {
            userId = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found"))
                    .getId();
        }

        return transactionService.filterByDateRange(userId, LocalDate.parse(start), LocalDate.parse(end), page, size);
    }


    @PostMapping("/upload-receipt")
    public ResponseEntity<ApiResponse<Transaction>> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam("transactionId") Long transactionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws IOException {

        Long userId = userService.getUserIdByEmail(userDetails.getUsername());

        Transaction updated = transactionService.attachReceipt(file, transactionId, userId);

        return ResponseEntity.ok(
                new ApiResponse<>("success", "Receipt uploaded", updated, 200)
        );
    }


    // save to disk -> uploads(folder)
//    @PostMapping("/upload-receipt")
//    public ResponseEntity<ApiResponse<String>> uploadReceipt(
//            @RequestParam("file") MultipartFile file) throws IOException {
//
//        Path uploadPath = Paths.get("uploads/");
//        if (!Files.exists(uploadPath)) {
//            Files.createDirectories(uploadPath);
//        }
//
//        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
//        Path filePath = uploadPath.resolve(fileName);
//
//        Files.copy(file.getInputStream(), filePath);
//
//        return ResponseEntity.ok(
//                new ApiResponse<>("success", "Receipt uploaded", fileName, 200));
//    }


}