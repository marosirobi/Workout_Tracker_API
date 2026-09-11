package com.marosirobi.workouttrackerapi.dto.workout;

import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record UpdateWorkoutRequest (@NotNull String workoutTitle,
                                    String workoutNote,
                                    LocalDateTime endTime,
                                    @Valid List<WorkoutExerciseRequest> exercises) { }
