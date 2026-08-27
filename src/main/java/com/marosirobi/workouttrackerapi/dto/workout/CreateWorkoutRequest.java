package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateWorkoutRequest {

    @NotNull(message = "User id can not be null!")
    private Long userId;

    @NotNull(message = "Workout type (STRENGTH/CARDIO) can not be null!")
    private WorkoutType type;

    @NotNull(message = "Start time can not be null!")
    private LocalDateTime startTime;

    private Long templateId;
}