package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record EditWorkoutRequest(@Size(max = 100) String workoutTitle,
                                 @Size(max = 255) String workoutNote,
                                 @Valid List<WorkoutExerciseRequest> exercises) {
}
