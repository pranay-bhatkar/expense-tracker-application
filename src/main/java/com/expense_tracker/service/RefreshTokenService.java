package com.expense_tracker.service;

import com.expense_tracker.exception.UserNotFoundException;
import com.expense_tracker.model.RefreshToken;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.RefreshTokenRepository;
import com.expense_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.el.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;


    @Value("${app.refresh-token.expiration-minutes:43200}")  // defaults 30 days
    private Long refreshTokenDurationMinutes;

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found with id : " + userId));

        // optionally delete old tokens for user (single token per user policy)
        refreshTokenRepository.deleteByUser(user);

        String tokenStr = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusSeconds(refreshTokenDurationMinutes * 60);

        RefreshToken rt = new RefreshToken(tokenStr, user, expiry);
        return refreshTokenRepository.save(rt);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now()) || token.isRevoked()) {
            return false;
        }
        return true;
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAllTokensForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }


    public Optional<RefreshToken> findTopByUser(User user) {
        return refreshTokenRepository.findTopByUserOrderByIdDesc(user);
    }

}