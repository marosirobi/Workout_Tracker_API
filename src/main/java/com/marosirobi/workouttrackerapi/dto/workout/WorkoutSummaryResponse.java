package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;

@Builder
public record WorkoutSummaryResponse(Long id,
                                     String name,
                                     WorkoutType type,
                                     Instant startTime,
                                     Instant endTime,

                                     Double totalDistance,
                                     String avgPace,
                                     Long durationSeconds) {
}
