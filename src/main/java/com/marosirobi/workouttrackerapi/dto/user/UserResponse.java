package com.marosirobi.workouttrackerapi.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Builder
public record UserResponse (Long id,
                            String username,
                            String email,
                            Double bodyWeightKg,
                            Instant createdAt,
                            String accessToken) { }
