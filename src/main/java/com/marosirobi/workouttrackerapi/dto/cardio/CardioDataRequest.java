package com.marosirobi.workouttrackerapi.dto.cardio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record CardioDataRequest(@NotNull String workoutTitle,
                                String workoutNote,
                                @NotNull CardioStatsResponse stats,
                                @Valid List<TrackPointResponse> trackPoints) {
}
