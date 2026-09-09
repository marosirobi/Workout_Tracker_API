package com.marosirobi.workouttrackerapi.dto.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.List;

@Builder
public record UpdateWorkoutTemplateRequest(@NotBlank String templateName, @Valid List<UpdateTemplateExerciseRequest> exercises) {
}
