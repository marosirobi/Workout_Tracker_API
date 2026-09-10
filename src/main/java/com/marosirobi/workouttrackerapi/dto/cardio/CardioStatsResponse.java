package com.marosirobi.workouttrackerapi.dto.cardio;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CardioStatsResponse(@NotNull Double totalDistance,
                                  String avgPace,
                                  Integer elevationGain,
                                  Integer movingTimeSeconds) {
}
