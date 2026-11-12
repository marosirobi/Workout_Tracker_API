package com.marosirobi.workouttrackerapi.dto.exercise;

import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import lombok.Builder;

import java.util.List;

@Builder
public record WorkoutExerciseResponse (Long id,
                                       Long exerciseId,
                                       String exerciseName,
                                       Integer orderIndex,
                                       Integer weightRank,
                                       MuscleGroup muscleGroup,
                                       TrackingType trackingType,
                                       List<ExerciseSetResponse> sets) { }
