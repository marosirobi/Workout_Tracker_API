package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateWorkoutRequest {
    private String workoutTitle;
    private String workoutNote;
    private LocalDateTime endTime;
    private List<WorkoutExerciseRequest> exercises;
}
