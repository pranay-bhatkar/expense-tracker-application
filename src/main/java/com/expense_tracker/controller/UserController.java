package com.expense_tracker.controller;

import com.expense_tracker.model.User;
import com.expense_tracker.response.ApiResponse;
import com.expense_tracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody User user) {
        User savedUser = userService.saveUser(user);
        ApiResponse<User> response = new ApiResponse<>(
                "success",
                "User created successfully",
                savedUser,
                HttpStatus.CREATED.value()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        ApiResponse<List<User>> response = new ApiResponse<>(
                "success",
                "Fetched all users successfully",
                users,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        ApiResponse<User> response = new ApiResponse<>(
                "success",
                "user fetched successfully",
                user,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    // put Replace all
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        User updateUser = userService.updateUser(id, user);
        ApiResponse<User> response = new ApiResponse<>(
                "success",
                "user updated successfully",
                updateUser,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    // patch Partial update
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        User updatedUser = userService.patchUser(id, updates);
        ApiResponse<User> response = new ApiResponse<>(
                "success",
                "user partially updated successfully",
                updatedUser,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        ApiResponse<Void> response = new ApiResponse<>(
                "success",
                "User deleted successfully",
                null,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }

    // delete all users
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAllUsers(@RequestParam(required = false) Boolean confirm) {
        if (confirm == null || !confirm) {
            ApiResponse<Void> response = new ApiResponse<>(
                    "warning",
                    "Are you sure you want to delete ALL users? Add '?confirm=true to confirm'",
                    null,
                    HttpStatus.BAD_REQUEST.value()
            );
            return ResponseEntity.badRequest().body(response);
        }

        userService.deleteAllUsers();
        ApiResponse<Void> response = new ApiResponse<>(
                "success",
                "All users deleted successfully",
                null,
                HttpStatus.OK.value()
        );
        return ResponseEntity.ok(response);
    }
}