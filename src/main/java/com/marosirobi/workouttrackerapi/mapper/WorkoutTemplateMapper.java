package com.marosirobi.workouttrackerapi.mapper;

import com.marosirobi.workouttrackerapi.domain.entity.WorkoutTemplate;
import com.marosirobi.workouttrackerapi.dto.template.TemplateExerciseResponse;
import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateResponse;
import org.springframework.stereotype.Component;

@Component
public class WorkoutTemplateMapper {

    public WorkoutTemplateResponse mapToResponse(WorkoutTemplate template){
        return WorkoutTemplateResponse.builder()
                .templateName(template.getTemplateName())
                .id(template.getId())
                .exercises(template.getExercises() == null ? null :
                        template.getExercises().stream().map(te -> TemplateExerciseResponse.builder()
                                .id(te.getId())
                                .orderIndex(te.getOrderIndex())
                                .targetSets(te.getTargetSets())
                                .exerciseId(te.getExercise() == null ? null : te.getExercise().getId())
                                .exerciseName(te.getExercise() == null ? null : te.getExercise().getExerciseName())
                                .build()
                        ).toList())
                .build();
    }
}
