package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Data;

import java.util.List;

@Data
public class WorkoutExerciseResponse {
    private Long id;
    private Long exerciseId;
    private String exerciseName;
    private Integer orderIndex;
    private List<ExerciseSetResponse> sets;
}
