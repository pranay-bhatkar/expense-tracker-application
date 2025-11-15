


package com.expense_tracker.controller;

import com.expense_tracker.dto.UserRequestDTO;
import com.expense_tracker.dto.UserResponseDTO;
import com.expense_tracker.model.Role;
import com.expense_tracker.model.User;
import com.expense_tracker.response.ApiResponse;
import com.expense_tracker.service.UserService;
import com.expense_tracker.utility.UserMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@Valid @RequestBody UserRequestDTO userRequest) {
        User userEntity = UserMapper.toEntity(userRequest);
        User saved = userService.saveUser(userEntity); // password is encoded in service
        UserResponseDTO dto = UserMapper.toDTO(saved);

        ApiResponse<UserResponseDTO> response = new ApiResponse<>("success", "User created successfully", dto, HttpStatus.CREATED.value());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET paginated (map entities -> DTOs)
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<User> userPage = userService.getAllUsers(page, size, sortBy, sortDir);

        Map<String, Object> responseData = new HashMap<>();
        List<UserResponseDTO> usersDto = userPage.getContent().stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());

        responseData.put("users", usersDto);
        responseData.put("currentPage", userPage.getNumber());
        responseData.put("totalItems", userPage.getTotalElements());
        responseData.put("totalPages", userPage.getTotalPages());
        responseData.put("pageSize", userPage.getSize());
        responseData.put("sortBy", sortBy);
        responseData.put("sortDir", sortDir);

        ApiResponse<Map<String, Object>> response = new ApiResponse<>("success", "Fetched paginated users successfully", responseData, HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    // GET by id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        UserResponseDTO dto = UserMapper.toDTO(user);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>("success", "User fetched successfully", dto, HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    // PUT (replace all) - accept UserRequestDTO (password optional behavior handled in service)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDTO userRequest) {
        User updateEntity = UserMapper.toEntity(userRequest);
        User updated = userService.updateUser(id, updateEntity);
        UserResponseDTO dto = UserMapper.toDTO(updated);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>("success", "User updated successfully", dto, HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    // PATCH (partial) - keep same signature (Map), but map response to DTO
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        User updated = userService.patchUser(id, updates);
        UserResponseDTO dto = UserMapper.toDTO(updated);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>("success", "User partially updated successfully", dto, HttpStatus.OK.value());
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

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserRole(
            @PathVariable Long id,
            @RequestParam Role role) {

        User updatedUser = userService.changeUserRole(id, role);
        UserResponseDTO dto = UserMapper.toDTO(updatedUser);

        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Role updated successfully",
                dto,
                HttpStatus.OK.value()
        ));
    }


}