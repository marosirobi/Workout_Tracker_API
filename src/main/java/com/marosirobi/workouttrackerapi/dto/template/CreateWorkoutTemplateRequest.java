package com.marosirobi.workouttrackerapi.dto.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateWorkoutTemplateRequest(@NotNull Long userId,
                                           @NotBlank String templateName,
                                           @Valid List<TemplateExerciseRequest> exercises) {
}
