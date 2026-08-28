package com.marosirobi.workouttrackerapi.dto.template;

import lombok.Builder;

@Builder
public record TemplateExerciseResponse(Long id,
                                       Integer targetSets,
                                       Integer orderIndex,
                                       String exerciseName,
                                       Long exerciseId) {
}
