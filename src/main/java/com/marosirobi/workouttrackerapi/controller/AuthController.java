package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.dto.auth.AuthResponse;
import com.marosirobi.workouttrackerapi.dto.auth.LoginRequest;
import com.marosirobi.workouttrackerapi.dto.auth.RefreshTokenRequest;
import com.marosirobi.workouttrackerapi.dto.user.CreateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UserResponse;
import com.marosirobi.workouttrackerapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CreateUserRequest request){
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request){
        return userService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request){
        return userService.refreshToken(request);
    }
}
