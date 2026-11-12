package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Builder;

@Builder
public record ExerciseSetResponse (Long id,
                                   Integer setNumber,
                                   Integer reps,
                                   Integer durationSeconds,
                                   Double weight) { }
