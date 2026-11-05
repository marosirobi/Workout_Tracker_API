package com.marosirobi.workouttrackerapi.dto.template;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TemplateExerciseRequest(@NotNull Long exerciseId,
                                      @NotNull Integer orderIndex,
                                      @Min(1) @Max(20) Integer targetSets) {
}
