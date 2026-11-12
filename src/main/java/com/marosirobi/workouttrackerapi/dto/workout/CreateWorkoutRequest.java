package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;

@Builder
public record CreateWorkoutRequest (@NotNull WorkoutType type,
                                    @NotNull Instant startTime,
                                    Long templateId) { }