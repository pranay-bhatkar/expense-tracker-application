package com.expense_tracker.controller.admin;


import com.expense_tracker.dto.admin.AdminDashboardDTO;
import com.expense_tracker.dto.user.UserResponseDTO;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.service.UserService;
import com.expense_tracker.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final TransactionRepository transactionRepository;


    private final AdminService adminService;

    // 1. List all users
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "false") boolean all
    ) {
        Page<UserResponseDTO> users = userService.getAllUsers(page, size, sortBy, sortDir, all);
        return ResponseEntity.ok(users);
    }


    // 2. System stats / dashboard
    @GetMapping("/analytics")
    public ResponseEntity<AdminDashboardDTO> getSystemStats() {
        AdminDashboardDTO dto = adminService.getDashboardStats(); // cached automatically
        return ResponseEntity.ok(dto);

    }
}