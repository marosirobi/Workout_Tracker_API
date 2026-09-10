package com.marosirobi.workouttrackerapi.dto.cardio;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TrackPointResponse(@NotNull Double latitude,
                                 @NotNull Double longitude,
                                 @NotNull Double elevation,
                                 @NotNull Double speed,
                                 @NotNull LocalDateTime timestamp) {
}
