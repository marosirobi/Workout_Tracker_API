package com.marosirobi.workouttrackerapi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
