package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.dto.auth.AuthResponse;
import com.marosirobi.workouttrackerapi.dto.auth.LoginRequest;
import com.marosirobi.workouttrackerapi.dto.auth.RefreshTokenRequest;
import com.marosirobi.workouttrackerapi.dto.user.CreateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UpdateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UserResponse;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Wrong email or password!"));

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new RuntimeException("Wrong email or password!");
        }

        String accessToken = jwtService.generateToken(user.getEmail(), user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(mapToResponse(user))
                .build();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request){

        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new RuntimeException("This email adress is already taken.");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(newUser);

        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(String email, UpdateUserRequest request){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        if(!user.getEmail().equals(request.getEmail())
                && userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new RuntimeException("This email is already taken.");
        }

        if(!user.getUsername().equals(request.getUsername())
                && userRepository.findByUsername(request.getUsername()).isPresent()){
            throw new RuntimeException("This username is already taken.");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(String email){
        if(!userRepository.existsByEmail(email)){
            throw new RuntimeException("User doesn't exist.");
        }
        userRepository.deleteByEmail(email);
    }

    public UserResponse getUserByEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));
        return mapToResponse(user);
    }

    public List<UserResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AuthResponse refreshToken(RefreshTokenRequest request){
        return refreshTokenService.findByToken(request.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = jwtService.generateToken(user.getEmail(), user.getId());

                    return AuthResponse.builder()
                            .token(newAccessToken)
                            .refreshToken(request.refreshToken())
                            .user(mapToResponse(user))
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Invalid Refresh Token"));
    }

    private UserResponse mapToResponse(User user){
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
