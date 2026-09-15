package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.RefreshToken;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.dto.auth.AuthResponse;
import com.marosirobi.workouttrackerapi.dto.auth.LoginRequest;
import com.marosirobi.workouttrackerapi.dto.auth.RefreshTokenRequest;
import com.marosirobi.workouttrackerapi.dto.user.CreateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UpdateUserRequest;
import com.marosirobi.workouttrackerapi.dto.user.UserResponse;
import com.marosirobi.workouttrackerapi.mapper.UserMapper;
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
    private final UserMapper userMapper;

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
                .user(userMapper.mapToResponse(user))
                .build();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request){

        if(userRepository.findByEmail(request.email()).isPresent()){
            throw new RuntimeException("This email adress is already taken.");
        }

        User newUser = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(newUser);

        return userMapper.mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(String email, UpdateUserRequest request){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        boolean credentialsChanged = false;

        if(!user.getEmail().equals(request.email())){
            if(userRepository.findByEmail(request.email()).isPresent())
                throw new RuntimeException("This email is already taken.");

            user.setEmail(request.email());
            credentialsChanged = true;
        }

        if(!user.getUsername().equals(request.username())){
            if(userRepository.findByUsername(request.username()).isPresent())
                throw new RuntimeException("This username is already taken.");

            user.setUsername(request.username());
            credentialsChanged = true;
        }

        User updatedUser = userRepository.save(user);

        String newAccessToken = credentialsChanged ? jwtService.generateToken(user.getEmail(), user.getId()) : null;

        return userMapper.mapToResponse(updatedUser, newAccessToken);
    }

    @Transactional
    public void deleteUser(String email){
        User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found " + email));

        userRepository.delete(user);
    }

    public UserResponse getUserByEmail(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        return userMapper.mapToResponse(user);
    }

    public List<UserResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(userMapper::mapToResponse)
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
                            .user(userMapper.mapToResponse(user))
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Invalid Refresh Token"));
    }
}
