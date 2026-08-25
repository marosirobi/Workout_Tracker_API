package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Data;

@Data
public class ExerciseSetRequest {
    private Integer setNumber;
    private Double weight;
    private Integer reps;
}
