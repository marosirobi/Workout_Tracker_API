package com.marosirobi.workouttrackerapi.dto.exercise;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record WorkoutExerciseRequest (@NotNull Long exerciseId,
                                      @NotNull Integer orderIndex,
                                      @Valid @NotEmpty List<ExerciseSetRequest> sets) { }
