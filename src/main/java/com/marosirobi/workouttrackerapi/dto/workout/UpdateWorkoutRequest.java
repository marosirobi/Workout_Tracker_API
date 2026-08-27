package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateWorkoutRequest {

    @NotNull(message = "Workout title can not be null!")
    private String workoutTitle;

    private String workoutNote;
    private LocalDateTime endTime;

    @Valid
    private List<WorkoutExerciseRequest> exercises;
}
