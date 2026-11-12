package com.marosirobi.workouttrackerapi.dto.cardio;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;

@Builder
public record TrackPointResponse(@NotNull Double latitude,
                                 @NotNull Double longitude,
                                 @NotNull Double elevation,
                                 @NotNull Double speed,
                                 Integer segment,
                                 @NotNull Instant timestamp) {
}
