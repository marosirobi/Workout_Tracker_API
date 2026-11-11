package com.marosirobi.workouttrackerapi.dto.stat;

import java.time.Instant;

public record ExerciseWorkoutBest(
        Long exerciseId,
        Long workoutId,
        Instant startTime,
        Double maxWeight,
        Integer maxReps,
        Integer maxDurationSeconds
) {}
