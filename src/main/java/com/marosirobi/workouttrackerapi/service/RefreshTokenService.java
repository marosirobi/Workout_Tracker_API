package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.repository.RefreshTokenRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final HandlerMapping resourceHandlerMapping;

    @Transactional
    public RefreshToken createRefreshToken(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found " +userId));

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(604800000L));

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken refreshToken){
        if(refreshToken.getExpiryDate().compareTo(Instant.now()) < 0){
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh Token is invalid");
        }
        return refreshToken;
    }

    public Optional<RefreshToken> findByToken(String token){
        return refreshTokenRepository.findByToken(token);
    }
}
