package com.marosirobi.workouttrackerapi.dto.exercise;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

@Builder
public record ExerciseSetRequest(@NotNull @Min(1) Integer setNumber,
                                 @NotNull @PositiveOrZero @Max(1000) Double weight,
                                 @NotNull @PositiveOrZero @Max(1000) Integer reps) { }
