package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.dto.auth.LoginRequest;
import com.marosirobi.workouttrackerapi.dto.auth.RefreshTokenRequest;
import com.marosirobi.workouttrackerapi.dto.user.ChangePasswordRequest;
import com.marosirobi.workouttrackerapi.dto.user.CreateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UpdateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UserResponse;
import com.marosirobi.workouttrackerapi.exception.BadRequestException;
import com.marosirobi.workouttrackerapi.exception.DuplicateResourceException;
import com.marosirobi.workouttrackerapi.exception.InvalidCredentialsException;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.mapper.UserMapper;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "user@gmail.com";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("User")
                .email(EMAIL)
                .password("hashed")
                .bodyWeightKg(80.0)
                .build();
    }

    private void givenTokensAreIssued() {
        given(jwtService.generateToken(anyString(), anyLong())).willReturn("access-token");
        given(refreshTokenService.createRefreshToken(any(User.class)))
                .willAnswer(inv -> RefreshToken.builder().token("refresh-token").user(inv.getArgument(0)).build());
        given(userMapper.mapToResponse(any(User.class)))
                .willAnswer(inv -> {
                    User u = inv.getArgument(0);
                    return UserResponse.builder().id(u.getId()).username(u.getUsername()).email(u.getEmail()).build();
                });
    }

    @Nested
    class CreateUser {

        @Test
        void normalizesInput_andReturnsTokens() {
            given(userRepository.existsByEmailIgnoreCase(EMAIL)).willReturn(false);
            given(userRepository.existsByUsernameIgnoreCase("User")).willReturn(false);
            given(passwordEncoder.encode("Password1!")).willReturn("hashed");
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            givenTokensAreIssued();

            var response = userService.createUser(new CreateUserRequest(" User ", " User@Gmail.COM ", "Password1!"));

            var saved = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(saved.capture());
            assertThat(saved.getValue())
                    .extracting(User::getUsername, User::getEmail, User::getPassword)
                    .containsExactly("User", EMAIL, "hashed");
            assertThat(response.token()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.user().email()).isEqualTo(EMAIL);
        }

        @Test
        void rejectsTakenEmail() {
            given(userRepository.existsByEmailIgnoreCase(EMAIL)).willReturn(true);

            assertThatThrownBy(() -> userService.createUser(new CreateUserRequest("User", "USER@gmail.com", "Password1!")))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasFieldOrPropertyWithValue("code", "EMAIL_TAKEN");
            then(userRepository).should(never()).save(any());
        }

        @Test
        void rejectsTakenUsername() {
            given(userRepository.existsByEmailIgnoreCase(EMAIL)).willReturn(false);
            given(userRepository.existsByUsernameIgnoreCase("user")).willReturn(true);

            assertThatThrownBy(() -> userService.createUser(new CreateUserRequest("user", EMAIL, "Password1!")))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasFieldOrPropertyWithValue("code", "USERNAME_TAKEN");
            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    class Login {

        @Test
        void acceptsDifferentlyCasedEmail() {
            given(userRepository.findByEmailIgnoreCase("USER@gmail.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", "hashed")).willReturn(true);
            givenTokensAreIssued();

            var response = userService.login(new LoginRequest(" USER@gmail.com ", "Password1!"));

            assertThat(response.token()).isEqualTo("access-token");
            then(refreshTokenService).should().createRefreshToken(user);
        }

        @Test
        void rejectsUnknownEmail() {
            given(userRepository.findByEmailIgnoreCase("nobody@gmail.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(new LoginRequest("nobody@gmail.com", "Password1!")))
                    .isInstanceOf(InvalidCredentialsException.class);
            then(passwordEncoder).shouldHaveNoInteractions();
            then(refreshTokenService).shouldHaveNoInteractions();
        }

        @Test
        void rejectsWrongPassword() {
            given(userRepository.findByEmailIgnoreCase(EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

            assertThatThrownBy(() -> userService.login(new LoginRequest(EMAIL, "wrong")))
                    .isInstanceOf(InvalidCredentialsException.class);
            then(refreshTokenService).shouldHaveNoInteractions();
        }
    }

    @Nested
    class UpdateUser {

        @Test
        void newEmail_issuesNewAccessToken() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(userRepository.existsByEmailIgnoreCase("new@mail.com")).willReturn(false);
            given(userRepository.save(user)).willReturn(user);
            given(jwtService.generateToken("new@mail.com", 1L)).willReturn("new-access");
            given(userMapper.mapToResponse(user, "new-access"))
                    .willReturn(UserResponse.builder().accessToken("new-access").build());

            var response = userService.updateUser(EMAIL, new UpdateUserRequest("User", " New@Mail.com ", 82.0));

            assertThat(user.getEmail()).isEqualTo("new@mail.com");
            assertThat(user.getBodyWeightKg()).isEqualTo(82.0);
            assertThat(response.accessToken()).isEqualTo("new-access");
        }

        @Test
        void onlyBodyWeight_keepsAccessToken() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
            given(userMapper.mapToResponse(user, null)).willReturn(UserResponse.builder().build());

            userService.updateUser(EMAIL, new UpdateUserRequest("User", EMAIL, 85.0));

            assertThat(user.getBodyWeightKg()).isEqualTo(85.0);
            then(jwtService).shouldHaveNoInteractions();
        }

        @Test
        void rejectsTakenEmail() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(userRepository.existsByEmailIgnoreCase("taken@mail.com")).willReturn(true);

            assertThatThrownBy(() -> userService.updateUser(EMAIL, new UpdateUserRequest("User", "taken@mail.com", 80.0)))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasFieldOrPropertyWithValue("code", "EMAIL_TAKEN");
            then(userRepository).should(never()).save(any());
        }

        @Test
        void usernameCaseChange_skipsDuplicateCheck() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(userRepository.save(user)).willReturn(user);
            given(jwtService.generateToken(EMAIL, 1L)).willReturn("new-access");
            given(userMapper.mapToResponse(user, "new-access")).willReturn(UserResponse.builder().build());

            userService.updateUser(EMAIL, new UpdateUserRequest("user", EMAIL, 80.0));

            assertThat(user.getUsername()).isEqualTo("user");
            then(userRepository).should(never()).existsByUsernameIgnoreCase(any());
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void revokesOtherSessions_beforeIssuingNewTokens() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Old12345!", "hashed")).willReturn(true);
            given(passwordEncoder.matches("New12345!", "hashed")).willReturn(false);
            given(passwordEncoder.encode("New12345!")).willReturn("new-hashed");
            givenTokensAreIssued();

            var response = userService.changePassword(EMAIL, new ChangePasswordRequest("Old12345!", "New12345!"));

            assertThat(user.getPassword()).isEqualTo("new-hashed");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            var inOrder = inOrder(refreshTokenService);
            then(refreshTokenService).should(inOrder).deleteAllForUser(user);
            then(refreshTokenService).should(inOrder).createRefreshToken(user);
        }

        @Test
        void rejectsWrongCurrentPassword() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

            assertThatThrownBy(() -> userService.changePassword(EMAIL, new ChangePasswordRequest("wrong", "New12345!")))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("code", "WRONG_CURRENT_PASSWORD");
            assertThat(user.getPassword()).isEqualTo("hashed");
            then(refreshTokenService).shouldHaveNoInteractions();
        }

        @Test
        void rejectsSamePassword() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Old12345!", "hashed")).willReturn(true);

            assertThatThrownBy(() -> userService.changePassword(EMAIL, new ChangePasswordRequest("Old12345!", "Old12345!")))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("code", "SAME_PASSWORD");
            then(passwordEncoder).should(never()).encode(any());
        }
    }

    @Nested
    class Session {

        @Test
        void refresh_returnsRotatedToken() {
            given(refreshTokenService.rotate("old-token"))
                    .willReturn(RefreshToken.builder().token("new-token").user(user).build());
            given(jwtService.generateToken(EMAIL, 1L)).willReturn("access-token");
            given(userMapper.mapToResponse(user)).willReturn(UserResponse.builder().id(1L).build());

            var response = userService.refreshToken(new RefreshTokenRequest("old-token"));

            assertThat(response.refreshToken()).isEqualTo("new-token");
            assertThat(response.token()).isEqualTo("access-token");
        }

        @Test
        void logout_deletesGivenToken() {
            userService.logout(new RefreshTokenRequest("some-token"));

            then(refreshTokenService).should().deleteByToken("some-token");
        }
    }

    @Nested
    class Account {

        @Test
        void delete_removesUser() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));

            userService.deleteUser(EMAIL);

            then(userRepository).should().delete(user);
        }

        @Test
        void get_unknownUser_throws() {
            given(userRepository.findByEmail("nobody@gmail.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("nobody@gmail.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
