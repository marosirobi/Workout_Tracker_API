package com.marosirobi.workouttrackerapi.dto.exercise;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record ExerciseSetRequest(@NotNull @Min(1) Integer setNumber,
                                 @NotNull @PositiveOrZero @Max(1000) Double weight,
                                 @PositiveOrZero @Max(36000) Integer durationSeconds,
                                 @NotNull @PositiveOrZero @Max(1000) Integer reps) { }
