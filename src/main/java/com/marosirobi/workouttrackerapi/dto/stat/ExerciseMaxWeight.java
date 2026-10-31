package com.marosirobi.workouttrackerapi.dto.stat;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ExerciseMaxWeight(Long exerciseId,
                                Long workoutId,
                                Instant startTime,
                                Double maxWeight) {
}
