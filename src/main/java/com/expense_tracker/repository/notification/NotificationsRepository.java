package com.expense_tracker.repository.notification;

import com.expense_tracker.model.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationsRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

}