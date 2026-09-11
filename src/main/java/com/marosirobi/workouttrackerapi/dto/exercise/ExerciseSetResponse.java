package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Builder;
import lombok.Data;

@Builder
public record ExerciseSetResponse (Long id,
                                   Integer setNumber,
                                   Integer reps,
                                   Double weight) { }
