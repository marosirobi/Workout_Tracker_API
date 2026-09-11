package com.marosirobi.workouttrackerapi.dto.exercise;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
public record ExerciseSetRequest(@NotNull @Min(value = 1) Integer setNumber,
                                 @Min(value = 0) Double weight,
                                 @Min(value = 0) Integer reps) { }
