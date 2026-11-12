package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record WorkoutSummaryResponse(Long id,
                                     String name,
                                     WorkoutType type,
                                     Instant startTime,
                                     Instant endTime,

                                     Double totalVolume,
                                     Integer totalSets,
                                     Integer exerciseCount,

                                     Double totalDistance,
                                     String avgPace,
                                     Integer movingTimeSeconds,
                                     Long durationSeconds) {
}
