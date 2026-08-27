package com.marosirobi.workouttrackerapi.dto.exercise;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExerciseSetRequest {

    @NotNull(message = "Number of sets can not be null!")
    @Min(value = 1, message = "Add at least 1 number of sets!")
    private Integer setNumber;

    @NotNull(message = "Weight can not be null!")
    @Min(value = 0, message = "Weight can not be negative!")
    private Double weight;

    @NotNull(message = "Reps can not be null!")
    @Min(value = 0, message = "Reps can not be negative!")
    private Integer reps;
}
