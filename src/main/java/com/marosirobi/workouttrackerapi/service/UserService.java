package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.dto.auth.AuthResponse;
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
import com.marosirobi.workouttrackerapi.utils.LogUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public AuthResponse login(LoginRequest request){
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> {
                    log.warn("Login failed: unknown email {}", LogUtils.maskEmail(request.email()));
                    return new InvalidCredentialsException("Wrong email or password!");
                });

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            log.warn("Login failed: wrong password, user={}", user.getId());
            throw new InvalidCredentialsException("Wrong email or password!");
        }

        log.info("User logged in, user={}", user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse createUser(CreateUserRequest request){

        String username = request.username().trim();
        String email = normalizeEmail(request.email());

        if(userRepository.existsByEmailIgnoreCase(email)){
            throw new DuplicateResourceException("This email address is already taken.", "EMAIL_TAKEN");
        }

        if(userRepository.existsByUsernameIgnoreCase(username)){
            throw new DuplicateResourceException("This username is already taken.", "USERNAME_TAKEN");
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("User registered, user={}", savedUser.getId());

        return buildAuthResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(String email, UpdateUserRequest request){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        String newUsername = request.username().trim();
        String newEmail = normalizeEmail(request.email());
        boolean credentialsChanged = false;

        if(!user.getEmail().equals(newEmail)){
            if(!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmailIgnoreCase(newEmail))
                throw new DuplicateResourceException("This email is already taken.", "EMAIL_TAKEN");

            user.setEmail(newEmail);
            credentialsChanged = true;
        }

        if(!user.getUsername().equals(newUsername)){
            if(!user.getUsername().equalsIgnoreCase(newUsername) && userRepository.existsByUsernameIgnoreCase(newUsername))
                throw new DuplicateResourceException("This username is already taken.", "USERNAME_TAKEN");

            user.setUsername(newUsername);
            credentialsChanged = true;
        }

        user.setBodyWeightKg(request.bodyWeightKg());

        User updatedUser = userRepository.save(user);
        log.info("Profile updated, user={}, credentialsChanged={}", user.getId(), credentialsChanged);

        String newAccessToken = credentialsChanged ? jwtService.generateToken(user.getEmail(), user.getId()) : null;

        return userMapper.mapToResponse(updatedUser, newAccessToken);
    }

    @Transactional
    public void deleteUser(String email){
        User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        userRepository.delete(user);
        log.info("Account deleted, user={}", user.getId());
    }

    public UserResponse getUserByEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        return userMapper.mapToResponse(user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request){
        RefreshToken newRefreshToken = refreshTokenService.rotate(request.refreshToken());
        User user = newRefreshToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .user(userMapper.mapToResponse(user))
                .build();
    }

    public void logout(RefreshTokenRequest request){
        refreshTokenService.deleteByToken(request.refreshToken());
    }

    @Transactional
    public AuthResponse changePassword(String email, ChangePasswordRequest request){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())){
            log.warn("Password change failed: wrong current password, user={}", user.getId());
            throw new BadRequestException("Current password is incorrect.", "WRONG_CURRENT_PASSWORD");
        }

        if(passwordEncoder.matches(request.newPassword(), user.getPassword())){
            throw new BadRequestException("New password must differ from the current one.", "SAME_PASSWORD");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        refreshTokenService.deleteAllForUser(user);
        log.info("Password changed, other sessions revoked, user={}", user.getId());
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user){
        String accessToken = jwtService.generateToken(user.getEmail(), user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(userMapper.mapToResponse(user))
                .build();
    }

    private static String normalizeEmail(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
