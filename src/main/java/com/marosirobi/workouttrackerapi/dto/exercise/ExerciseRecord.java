package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ExerciseRecord(Double value,
                             Long workoutId,
                             Instant date) {
}
