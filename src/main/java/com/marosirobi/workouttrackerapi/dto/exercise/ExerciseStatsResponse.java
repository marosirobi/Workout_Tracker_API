package com.marosirobi.workouttrackerapi.dto.exercise;

import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;

import java.util.List;

public record ExerciseStatsResponse(
        Long exerciseId,
        String exerciseName,
        MuscleGroup muscleGroup,
        TrackingType trackingType,
        ExerciseRecord heaviestWeight,
        ExerciseRecord bestOneRepMax,
        ExerciseRecord bestSetVolume,
        ExerciseRecord bestSessionVolume,
        ExerciseRecord mostReps,
        ExerciseRecord longestDuration,
        List<ExerciseSessionStat> history
) {}
