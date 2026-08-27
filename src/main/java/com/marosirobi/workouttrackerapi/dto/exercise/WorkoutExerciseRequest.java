package com.marosirobi.workouttrackerapi.dto.exercise;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WorkoutExerciseRequest {

    @NotNull(message = "Exercise id can not be null!")
    private Long exerciseId;

    @NotNull(message = "Order index can not be null!")
    private Integer orderIndex;

    @Valid
    private List<ExerciseSetRequest> sets;
}
