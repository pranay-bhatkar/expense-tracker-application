package com.expense_tracker.controller.notification;

import com.expense_tracker.model.User;
import com.expense_tracker.model.notification.Notification;
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
    public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications() {
        User user = userService.getCurrentUser();

        List<Notification> notifications = notificationService.getUnreadNotifications(user);

        ApiResponse<List<Notification>> response = new ApiResponse<>(
                "success",
                "successfully get the notifications",
                notifications,
                HttpStatus.CREATED.value()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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


}