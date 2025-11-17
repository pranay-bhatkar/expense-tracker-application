package com.expense_tracker.service;


import com.expense_tracker.model.PasswordResetOtp;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.PasswordResetOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;


    public void generateOtp(User user) {
        String otp = String.format("%6d", new Random().nextInt(999999));

        PasswordResetOtp resetOtp = new PasswordResetOtp();

        resetOtp.setOtp(otp);
        resetOtp.setUser(user);
        resetOtp.setExpiryTime(LocalDateTime.now().plusMinutes(10));

        otpRepository.save(resetOtp);

        // Send OTP with Thymeleaf template
        emailService.sendOtpEmail(
                user.getEmail(),
                user.getName(),
                otp
        );
    }


    public boolean veryOtp(User user, String otp) {
        return otpRepository.findByOtpAndUser(otp, user)
                .filter(o -> o.getExpiryTime().isAfter(LocalDateTime.now()))
                .isPresent();
    }

}