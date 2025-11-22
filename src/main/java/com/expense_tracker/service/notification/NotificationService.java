package com.expense_tracker.service.notification;

import com.expense_tracker.model.User;
import com.expense_tracker.model.notification.Notification;
import com.expense_tracker.repository.notification.NotificationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationsRepository notificationsRepository;

    public void sendNotification(User user, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .read(false)
                .build();
        notificationsRepository.save(notification);
    }

    public List<Notification> getUnreadNotifications(User user) {
        return notificationsRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
    }

    public void markAsRead(Long notificationId) {
        notificationsRepository.findById(notificationId).ifPresent(
                n -> {
                    n.setRead(true);
                    notificationsRepository.save(n);
                }
        );
    }
}