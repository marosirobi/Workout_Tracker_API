package com.marosirobi.workouttrackerapi.dto.template;

import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import lombok.Builder;

@Builder
public record TemplateExerciseResponse(Long id,
                                       Integer targetSets,
                                       Integer orderIndex,
                                       String exerciseName,
                                       MuscleGroup muscleGroup,
                                       Long exerciseId) {
}
