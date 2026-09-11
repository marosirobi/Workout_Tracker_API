package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
public record WorkoutExerciseResponse (Long id,
                                       Long exerciseId,
                                       String exerciseName,
                                       Integer orderIndex,
                                       List<ExerciseSetResponse> sets) { }
