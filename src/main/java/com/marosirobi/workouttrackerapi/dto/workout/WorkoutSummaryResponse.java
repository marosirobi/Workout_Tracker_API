package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WorkoutSummaryResponse(Long id,
                                     String name,
                                     WorkoutType type,
                                     LocalDateTime startTime,
                                     LocalDateTime endTime,

                                     Double totalDistance,
                                     String avgPace,
                                     Long durationSeconds) {
}
