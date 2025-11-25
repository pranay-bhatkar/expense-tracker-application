package com.expense_tracker.controller.notification;

import com.expense_tracker.dto.notification.NotificationResponseDTO;
import com.expense_tracker.model.User;
import com.expense_tracker.response.ApiResponse;
import com.expense_tracker.service.UserService;
import com.expense_tracker.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;


    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getUnreadNotifications() {
        User user = userService.getCurrentUser();

        List<NotificationResponseDTO> dtoList = notificationService.getUnreadNotifications(user);

        ApiResponse<List<NotificationResponseDTO>> response = new ApiResponse<>(
                "success",
                "Successfully retrieved notifications",
                dtoList,
                HttpStatus.OK.value()
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/read/{id}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);

        ApiResponse<Void> response = new ApiResponse<>(
                "success",
                "Notification marked as read",
                null,
                HttpStatus.CREATED.value()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        User user = userService.getCurrentUser();
        notificationService.markAllAsRead(user);
        ApiResponse<String> response = new ApiResponse<>(
                "success",
                "All notifications marked as read",
                null,
                HttpStatus.CREATED.value()
        );

        return ResponseEntity.ok(response);
    }

}