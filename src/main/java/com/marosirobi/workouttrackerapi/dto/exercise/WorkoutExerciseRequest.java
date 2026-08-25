package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Data;

import java.util.List;

@Data
public class WorkoutExerciseRequest {
    private Long exerciseId;
    private Integer orderIndex;
    private List<ExerciseSetRequest> sets;
}
