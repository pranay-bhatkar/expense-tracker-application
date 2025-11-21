package com.expense_tracker.repository;

import com.expense_tracker.model.PasswordResetOtp;
import com.expense_tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository
        <PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findByOtpAndUser(String otp, User user);

    void deleteByUser(User user);

    Optional<PasswordResetOtp> findByUser(User user);
}