package com.marosirobi.workouttrackerapi.dto.exercise;

import java.time.Instant;

public record ExerciseSessionStat(
        Long workoutId,
        Instant startTime,
        Double maxWeight,
        Double bestOneRepMax,
        Double bestSetVolume,
        Double bestSessionVolume,
        Integer maxReps,
        Integer maxDurationSeconds
) {}
