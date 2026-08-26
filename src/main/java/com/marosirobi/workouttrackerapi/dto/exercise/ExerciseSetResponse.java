package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Data;

@Data
public class ExerciseSetResponse {
    private Long id;
    private Integer setNumber;
    private Integer reps;
    private Double weight;
}
