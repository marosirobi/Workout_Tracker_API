package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
public record WorkoutExerciseResponse (Long id,
                                       Long exerciseId,
                                       String exerciseName,
                                       Integer orderIndex,
                                       Integer weightRank,
                                       List<ExerciseSetResponse> sets) { }
