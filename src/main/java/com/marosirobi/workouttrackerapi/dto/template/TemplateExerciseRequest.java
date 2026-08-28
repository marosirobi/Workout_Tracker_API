package com.marosirobi.workouttrackerapi.dto.template;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TemplateExerciseRequest(@NotNull Long exerciseId,
                                      @NotNull Integer orderIndex,
                                      @NotNull Integer targetSets) {
}
