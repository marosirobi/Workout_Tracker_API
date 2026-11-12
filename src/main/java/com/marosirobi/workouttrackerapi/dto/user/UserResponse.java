package com.marosirobi.workouttrackerapi.dto.user;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UserResponse (Long id,
                            String username,
                            String email,
                            Double bodyWeightKg,
                            Instant createdAt,
                            String accessToken) { }
