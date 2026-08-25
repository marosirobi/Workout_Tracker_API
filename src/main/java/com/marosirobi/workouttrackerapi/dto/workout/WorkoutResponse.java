package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkoutResponse {
    private Long id;
    private Long templateId;
    private String workoutTitle;
    private String workoutNote;
    private WorkoutType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
