package com.expense_tracker.service;


import com.expense_tracker.constants.SecurityConstants;
import com.expense_tracker.dto.AuthResponse;
import com.expense_tracker.dto.LoginRequest;
import com.expense_tracker.exception.*;
import com.expense_tracker.model.RefreshToken;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.UserRepository;
import com.expense_tracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not Found"));

        String role = user.getRole() != null ? user.getRole().name() : "USER";

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + role)  // e.g. ROLE_USER or ROLE_ADMIN
                .build();
    }


//    public User registerUser(User user) {
//        user.setRole("USER");
//        return userService.saveUser(user);
//    }


    public void increaseFailedAttempts(User user) {
        int newFailedAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newFailedAttempts);
        userRepository.save(user);
    }

    public void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        userRepository.save(user);
    }

    public void lockUser(User user) {
        user.setAccountLocked(true);
        user.setLockTime(LocalDateTime.now());
        userRepository.save(user);
    }

    public boolean unlockWhenTimeExpired(User user) {
        LocalDateTime lockTime = user.getLockTime();

        if (lockTime == null) return false;

        if (lockTime.plusMinutes(SecurityConstants.LOCK_TIME_DURATION).isBefore(LocalDateTime.now())) {
            user.setAccountLocked(false);
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }


    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found "));

        // check if account is locked
        if (user.isAccountLocked() && !unlockWhenTimeExpired(user)) {
            throw new AccountLockedException("Account is locked. Try again after " + SecurityConstants.LOCK_TIME_DURATION + " minutes" +
                    ".");
        }


        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            increaseFailedAttempts(user);
            if (user.getFailedAttempts() >= SecurityConstants.MAX_FAILED_ATTEMPTS) {
                lockUser(user);
                throw new AccountLockedException(
                        "Account locked due to too many failed attempts"
                );
            }
            throw new InvalidCredentialException("Invalid credentials");
        }

        // Successful login - reset attempts
        resetFailedAttempts(user);

        // Check for existing valid refresh token
        RefreshToken existingToken = refreshTokenService.findTopByUser(user)
                .filter(rt -> refreshTokenService.verifyExpiration(rt))
                .orElseGet(() -> refreshTokenService.createRefreshToken(user.getId()));

        // generate tokens
        String accessToken = jwtService.generateToken(user.getEmail());
//        String refreshToken = refreshTokenService.createRefreshToken(user.getId()).getToken();

        // get the refresh token string from the object
        String refreshToken = existingToken.getToken();

        return new AuthResponse(accessToken, refreshToken, jwtService.getExpiryInSeconds());
    }

    // refresh token
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken rt = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!refreshTokenService.verifyExpiration(rt)) {
            refreshTokenService.revoke(rt);
            throw new TokenExpiredException("Refresh token expired");
        }

        refreshTokenService.revoke(rt); // revoke old
        User user = rt.getUser();
        RefreshToken newRt = refreshTokenService.createRefreshToken(user.getId());
        String newAccessToken = jwtService.generateToken(user.getEmail());

        return new AuthResponse(newAccessToken, newRt.getToken(), jwtService.getExpiryInSeconds());
    }

    public void logout(String refreshToken) {
        RefreshToken rt = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        refreshTokenService.revoke(rt);
    }


}