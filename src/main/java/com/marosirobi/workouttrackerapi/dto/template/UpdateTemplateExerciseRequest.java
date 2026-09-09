package com.marosirobi.workouttrackerapi.dto.template;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateTemplateExerciseRequest(@NotNull Long exerciseId,
                                            @NotNull Integer orderIndex,
                                            @Min(value = 1) Integer targetSets) {
}
