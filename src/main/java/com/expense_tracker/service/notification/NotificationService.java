package com.expense_tracker.service.notification;

import com.expense_tracker.dto.notification.NotificationResponseDTO;
import com.expense_tracker.exception.ResourceNotFoundException;
import com.expense_tracker.model.User;
import com.expense_tracker.model.notification.Notification;
import com.expense_tracker.repository.notification.NotificationsRepository;
import com.expense_tracker.utility.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationsRepository notificationsRepository;
    private final JavaMailSender mailSender;

    // In-app + email notification
    @Async
    public void sendNotification(User user, String title, String message) {
        // save in-app notification
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .read(false)
                .build();

        notificationsRepository.save(notification);

        // send email notifications
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject(title);
            mailMessage.setText(message);
            mailSender.send(mailMessage);
        }
    }

    public List<NotificationResponseDTO> getUnreadNotifications(User user) {
        return notificationsRepository.
                findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationMapper::toDTO)
                .toList();
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationsRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification with id " + notificationId + " not found"
                ));
        notification.setRead(true);
        notificationsRepository.save(notification);

    }


    public void markAllAsRead(User user) {
        List<Notification> unread = notificationsRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());

        unread.forEach(n -> n.setRead(true));

        notificationsRepository.saveAll(unread);
    }

    public void deleteNotification(Long notificationId) {
        Notification notification = notificationsRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification with id " + notificationId + " not found"
                ));
        notificationsRepository.delete(notification);
    }







}