package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.dto.auth.AuthResponse;
import com.marosirobi.workouttrackerapi.dto.auth.LoginRequest;
import com.marosirobi.workouttrackerapi.dto.auth.RefreshTokenRequest;
import com.marosirobi.workouttrackerapi.dto.user.CreateUserRequest;
import com.marosirobi.workouttrackerapi.exception.TooManyRequestsException;
import com.marosirobi.workouttrackerapi.service.UserService;
import com.marosirobi.workouttrackerapi.utils.LogUtils;
import com.marosirobi.workouttrackerapi.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RateLimiter rateLimiter;

    @Value("${app.rate-limit.login-per-15-min}")
    private int loginLimit;

    @Value("${app.rate-limit.register-per-hour}")
    private int registerLimit;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest){
        if (!rateLimiter.tryAcquire("register:" + httpRequest.getRemoteAddr(), registerLimit, Duration.ofHours(1))) {
            log.warn("Too many registrations from {}", httpRequest.getRemoteAddr());
            throw new TooManyRequestsException("Too many registrations, try again later.");
        }
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request){
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!rateLimiter.tryAcquire("login:" + email, loginLimit, Duration.ofMinutes(15))) {
            log.warn("Too many login attempts for {}", LogUtils.maskEmail(email));
            throw new TooManyRequestsException("Too many login attempts, try again later.");
        }
        return userService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request){
        return userService.refreshToken(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request){
        userService.logout(request);
    }
}
