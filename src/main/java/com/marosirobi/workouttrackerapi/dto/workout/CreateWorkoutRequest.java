package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Builder
public record CreateWorkoutRequest (@NotNull WorkoutType type,
                                    @NotNull Instant startTime,
                                    Long templateId) { }