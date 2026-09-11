package com.marosirobi.workouttrackerapi.dto.template;

import lombok.Builder;

import java.util.List;

@Builder
public record WorkoutTemplateResponse(Long id, String templateName,
                                      List<TemplateExerciseResponse> exercises) {
}
