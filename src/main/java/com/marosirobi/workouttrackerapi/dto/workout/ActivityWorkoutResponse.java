package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ActivityWorkoutResponse(Long id,
                                      WorkoutType type,
                                      Instant startTime) {
}
