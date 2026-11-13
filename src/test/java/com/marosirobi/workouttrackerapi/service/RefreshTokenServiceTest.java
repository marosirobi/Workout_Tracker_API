package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.exception.TokenRefreshException;
import com.marosirobi.workouttrackerapi.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration LIFETIME = Duration.ofDays(7);

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        // @Value field, not injected in unit tests
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", LIFETIME.toMillis());
        user = User.builder().id(1L).email("user@gmail.com").build();
    }

    private void givenSaveReturnsArgument() {
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private RefreshToken token(String value, Instant expiry) {
        return RefreshToken.builder().token(value).user(user).expiryDate(expiry).build();
    }

    @Nested
    class Create {

        @Test
        void issuesUniqueToken_withSevenDayExpiry() {
            givenSaveReturnsArgument();
            Instant now = Instant.now();

            var first = refreshTokenService.createRefreshToken(user);
            var second = refreshTokenService.createRefreshToken(user);

            assertThat(first.getToken()).isNotEqualTo(second.getToken());
            assertThat(first.getUser()).isSameAs(user);
            assertThat(first.getExpiryDate()).isCloseTo(now.plus(LIFETIME), within(5, ChronoUnit.SECONDS));
        }
    }

    @Nested
    class Rotate {

        @Test
        void replacesValidToken() {
            var existing = token("old", Instant.now().plusSeconds(3600));
            given(refreshTokenRepository.findByToken("old")).willReturn(Optional.of(existing));
            givenSaveReturnsArgument();

            var rotated = refreshTokenService.rotate("old");

            then(refreshTokenRepository).should().delete(existing);
            assertThat(rotated.getToken()).isNotEqualTo("old");
            assertThat(rotated.getUser()).isSameAs(user);
        }

        @Test
        void rejectsUnknownToken() {
            given(refreshTokenRepository.findByToken("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.rotate("unknown"))
                    .isInstanceOf(TokenRefreshException.class);
            then(refreshTokenRepository).should(never()).delete(any());
            then(refreshTokenRepository).should(never()).save(any());
        }

        @Test
        void deletesExpiredToken_andRejectsIt() {
            var expired = token("old", Instant.now().minusSeconds(60));
            given(refreshTokenRepository.findByToken("old")).willReturn(Optional.of(expired));

            assertThatThrownBy(() -> refreshTokenService.rotate("old"))
                    .isInstanceOf(TokenRefreshException.class);
            then(refreshTokenRepository).should().delete(expired);
            then(refreshTokenRepository).should(never()).save(any());
        }
    }

    @Nested
    class Delete {

        @Test
        void byToken_deletesExisting() {
            var existing = token("t", Instant.now().plusSeconds(60));
            given(refreshTokenRepository.findByToken("t")).willReturn(Optional.of(existing));

            refreshTokenService.deleteByToken("t");

            then(refreshTokenRepository).should().delete(existing);
        }

        @Test
        void byToken_ignoresUnknown() {
            given(refreshTokenRepository.findByToken("unknown")).willReturn(Optional.empty());

            assertThatNoException().isThrownBy(() -> refreshTokenService.deleteByToken("unknown"));
            then(refreshTokenRepository).should(never()).delete(any());
        }

        @Test
        void allForUser() {
            refreshTokenService.deleteAllForUser(user);

            then(refreshTokenRepository).should().deleteAllByUser(user);
        }

        @Test
        void expired_usesCurrentTime() {
            given(refreshTokenRepository.deleteAllByExpiryDateBefore(any(Instant.class))).willReturn(3L);
            Instant before = Instant.now();

            refreshTokenService.deleteExpiredTokens();

            then(refreshTokenRepository).should().deleteAllByExpiryDateBefore(
                    argThat(cutoff -> !cutoff.isBefore(before) && !cutoff.isAfter(Instant.now())));
        }
    }
}
