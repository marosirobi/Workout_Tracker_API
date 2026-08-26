package com.marosirobi.workouttrackerapi.dto.workout;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@JsonPropertyOrder({
        "id",
        "templateId",
        "startTime",
        "endTime",
        "type",
        "workoutTitle",
        "workoutNote",
        "exercises"
})
@Data
public class WorkoutResponse {
    private Long id;
    private Long templateId;
    private String workoutTitle;
    private String workoutNote;
    private WorkoutType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private List<WorkoutExerciseResponse> exercises;
}
