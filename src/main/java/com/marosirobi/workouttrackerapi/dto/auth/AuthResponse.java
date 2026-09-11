package com.marosirobi.workouttrackerapi.dto.auth;

import com.marosirobi.workouttrackerapi.dto.user.UserResponse;
import lombok.Builder;

@Builder
public record AuthResponse(String token,
                           String refreshToken,
                           UserResponse user) { }
