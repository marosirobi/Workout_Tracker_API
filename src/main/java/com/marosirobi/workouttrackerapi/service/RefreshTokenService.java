package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.exception.TokenRefreshException;
import com.marosirobi.workouttrackerapi.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user){
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(dontRollbackOn = TokenRefreshException.class)
    public RefreshToken rotate(String oldToken){
        RefreshToken existing = refreshTokenRepository.findByToken(oldToken)
                .orElseThrow(() -> {
                    log.warn("Refresh failed: unknown refresh token");
                    return new TokenRefreshException("Invalid refresh token");
                });

        refreshTokenRepository.delete(existing);

        if(existing.getExpiryDate().isBefore(Instant.now())){
            log.info("Refresh failed: expired refresh token, user={}", existing.getUser().getId());
            throw new TokenRefreshException("Refresh token expired");
        }

        log.debug("Refresh token rotated, user={}", existing.getUser().getId());

        return createRefreshToken(existing.getUser());
    }

    @Transactional
    public void deleteByToken(String token){
        refreshTokenRepository.findByToken(token)
                .ifPresentOrElse(
                        refreshToken -> {
                            refreshTokenRepository.delete(refreshToken);
                            log.info("User logged out, user={}", refreshToken.getUser().getId());
                        },
                        () -> log.debug("Logout with unknown refresh token"));
    }

    @Transactional
    public void deleteAllForUser(User user){
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTokens(){
        long deleted = refreshTokenRepository.deleteAllByExpiryDateBefore(Instant.now());
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        }
    }
}
