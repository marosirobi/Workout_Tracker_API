package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.exception.TokenRefreshException;
import com.marosirobi.workouttrackerapi.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

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
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));

        refreshTokenRepository.delete(existing);

        if(existing.getExpiryDate().isBefore(Instant.now())){
            throw new TokenRefreshException("Refresh token expired");
        }

        return createRefreshToken(existing.getUser());
    }

    @Transactional
    public void deleteByToken(String token){
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void deleteAllForUser(User user){
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTokens(){
        refreshTokenRepository.deleteAllByExpiryDateBefore(Instant.now());
    }
}
