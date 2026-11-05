package com.marosirobi.workouttrackerapi.dto.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateWorkoutTemplateRequest(@NotBlank @Size(max=100) String templateName,
                                           @Valid List<TemplateExerciseRequest> exercises) {
}
