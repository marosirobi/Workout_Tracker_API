package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.exception.TokenRefreshException;
import com.marosirobi.workouttrackerapi.repository.RefreshTokenRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshToken_Success(){
        Long userId = 1L;
        User user = User.builder()
                .id(1L)
                .build();

        RefreshToken savedRToken = RefreshToken.builder()
                .id(100L)
                .user(user)
                .token("refresh-token")
                .expiryDate(Instant.now().plusMillis(604800000L))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRToken);

        RefreshToken result = refreshTokenService.createRefreshToken(userId);

        assertNotNull(result);
        assertEquals("refresh-token", result.getToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_ThrowsResourceNotFoundException_WhenUserDoesNotExist(){
        Long userId = 99L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> refreshTokenService.createRefreshToken(userId));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void verifyExpiration_Success(){
        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-token")
                .expiryDate(Instant.now().plusMillis(604800000L))
                .build();

        RefreshToken result = refreshTokenService.verifyExpiration(refreshToken);
        assertNotNull(result);
        assertEquals("valid-token", result.getToken());
        verify(refreshTokenRepository, never()).delete(refreshToken);
    }

    @Test
    void verifyExpiration_ThrowsTokenRefreshException_WhenTokenIsExpired(){
        RefreshToken refreshToken = RefreshToken.builder()
                .token("invalid-token")
                .expiryDate(Instant.now().minusMillis(3600))
                .build();

        assertThrows(TokenRefreshException.class, () -> refreshTokenService.verifyExpiration(refreshToken));
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
    }

    @Test
    void findByToken_Success(){
        String token = "myToken";
        RefreshToken foundToken = RefreshToken.builder()
                .token(token)
                .build();

        Optional<RefreshToken> result = refreshTokenService.findByToken(token);
        assertTrue(result.isPresent());
        assertEquals(token, result.get().getToken());
    }

}
