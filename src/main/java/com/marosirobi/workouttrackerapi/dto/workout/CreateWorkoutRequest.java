package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateWorkoutRequest {
    private Long userId;
    private Long templateId;
    private WorkoutType type;
    private LocalDateTime startTime;
}
