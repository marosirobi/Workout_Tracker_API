package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.dto.auth.AuthResponse;
import com.marosirobi.workouttrackerapi.dto.user.ChangePasswordRequest;
import com.marosirobi.workouttrackerapi.dto.user.UpdateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UserResponse;
import com.marosirobi.workouttrackerapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/me")
    public UserResponse updateMyAccount(Principal principal,
                                        @Valid @RequestBody UpdateUserRequest request){
        return userService.updateUser(principal.getName(), request);
    }

    @PutMapping("/me/password")
    public AuthResponse changeMyPassword(Principal principal,
                                         @Valid @RequestBody ChangePasswordRequest request){
        return userService.changePassword(principal.getName(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(Principal principal){
        userService.deleteUser(principal.getName());
    }

    @GetMapping("/me")
    public UserResponse getMyAccount(Principal principal){
        return userService.getUserByEmail(principal.getName());
    }

}
