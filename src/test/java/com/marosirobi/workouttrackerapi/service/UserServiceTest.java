package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.dto.auth.AuthResponse;
import com.marosirobi.workouttrackerapi.dto.auth.LoginRequest;
import com.marosirobi.workouttrackerapi.dto.user.CreateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UpdateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UserResponse;
import com.marosirobi.workouttrackerapi.exception.DuplicateResourceException;
import com.marosirobi.workouttrackerapi.exception.InvalidCredentialsException;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.mapper.UserMapper;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_Success(){
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test@example.com")
                .username("user")
                .password("password")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("user")
                .password("password")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .username("user")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.mapToResponse(savedUser)).thenReturn(userResponse);

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("test@example.com",result.email());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_ThrowsDuplicateResourceException_WhenEmailTaken(){
        CreateUserRequest request = CreateUserRequest.builder()
                .email("exists@example.com")
                .username("user")
                .password("password")
                .build();

        User existingUser = User.builder()
                .id(1L)
                .email("exists@example.com")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success(){
        LoginRequest request = LoginRequest.builder()
                .email("test@test.com")
                .password("password")
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encodedPassword")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token")
                .build();

        UserResponse response = UserResponse.builder()
                .email("test@test.com")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(),user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getEmail(),user.getId())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);
        when(userMapper.mapToResponse(user)).thenReturn(response);

        AuthResponse result = userService.login(request);

        assertNotNull(result);
        assertEquals("access-token", result.token());
        assertEquals("refresh-token", result.refreshToken());
    }

    @Test
    void login_ThrowsInvalidCredentialsException_WhenEmailNotFound(){
        LoginRequest request = LoginRequest.builder()
                .email("test@test.com")
                .password("password")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void login_ThrowsInvalidCredentialsException_WhenPasswordIsWrong(){
        LoginRequest request = LoginRequest.builder()
                .email("test@test.com")
                .password("password")
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encodedPassword")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    void updateUser_Success(){
        String oldEmail = "old@test.com";
        String oldUser = "oldUser";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("new@test.com")
                .username("newUser")
                .build();

        User user = User.builder()
                .id(1L)
                .email(oldEmail)
                .username(oldUser)
                .build();

        UserResponse response = UserResponse.builder()
                .email("new@test.com")
                .username("newUser")
                .build();

        when(userRepository.findByEmail(oldEmail)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(request.email()).isPresent()).thenReturn(false);
        when(userRepository.findByUsername(request.username()).isPresent()).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken("new@test.com", user.getId())).thenReturn("new-access-token");
        when(userMapper.mapToResponse(user,"new-access-token")).thenReturn(response);

        UserResponse result = userService.updateUser(oldEmail, request);

        assertNotNull(result);
        assertEquals("new@test.com",result.email());
        verify(jwtService, times(1)).generateToken(anyString(), anyLong());
    }

    @Test
    void updateUser_ThrowsDuplicateResourceException_WhenNewEmailIsTaken(){
        String oldEmail = "old@test.com";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("taken@test.com")
                .build();

        User existingUser = User.builder()
                .id(1L)
                .email(oldEmail)
                .build();

        User otherUser = User.builder()
                .id(2L)
                .email("taken@test.com")
                .build();

        when(userRepository.findByEmail(oldEmail)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(otherUser));

        assertThrows(DuplicateResourceException.class,() -> userService.updateUser(oldEmail, request));
    }

    @Test
    void updateUser_ThrowsDuplicateResourceException_WhenNewUsernameIsTaken(){
        String oldEmail = "old@test.com";
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("new@test.com")
                .username("updateUser")
                .build();

        User existingUser = User.builder()
                .id(1L)
                .username("oldUser")
                .email(oldEmail)
                .build();

        User otherUser = User.builder()
                .id(2L)
                .username("updateUser")
                .email("taken@test.com")
                .build();

        when(userRepository.findByEmail(oldEmail)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(request.email()).isPresent()).thenReturn(false);
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(otherUser));

        assertThrows(DuplicateResourceException.class,() -> userService.updateUser(oldEmail, request));
    }

    @Test
    void deleteUser_Success(){
        String email = "test@test.com";
        User user = User.builder()
                .id(1L)
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        userService.deleteUser(email);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void getUserByEmail_Success(){
        String email = "test@test.com";

        User user = User.builder()
                .id(1L)
                .email(email)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.mapToResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(email,result.email());

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void getUserByEmail_ThrowsResourceNotFoundException_WhenUserDoesNotExist(){
        String email = "test@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserByEmail(email)
        );

        verify(userMapper, never()).mapToResponse(any());
    }

}
