package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.dto.template.CreateWorkoutTemplateRequest;
import com.marosirobi.workouttrackerapi.dto.template.TemplateExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.template.TemplateExerciseResponse;
import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateResponse;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.TemplateExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import com.marosirobi.workouttrackerapi.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutTemplateResponse createTemplate(CreateWorkoutTemplateRequest request, String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        WorkoutTemplate workoutTemplate = new WorkoutTemplate();
        workoutTemplate.setTemplateName(request.templateName());
        workoutTemplate.setUser(user);

        if(request.exercises() != null){
            for(TemplateExerciseRequest exReq : request.exercises()){
                Exercise exercise = exerciseRepository.findById(exReq.exerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found " + exReq.exerciseId()));

                TemplateExercise templateExercise = new TemplateExercise();
                templateExercise.setExercise(exercise);
                templateExercise.setOrderIndex(exReq.orderIndex());
                templateExercise.setTargetSets(exReq.targetSets());
                templateExercise.setWorkoutTemplate(workoutTemplate);

                workoutTemplate.getExercises().add(templateExercise);
            }
        }

        WorkoutTemplate savedTemplate = workoutTemplateRepository.save(workoutTemplate);
        return mapToResponse(savedTemplate);
    }

    public List<WorkoutTemplateResponse> getTemplatesByUserId(Long userId){
        return workoutTemplateRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private WorkoutTemplateResponse mapToResponse(WorkoutTemplate template){
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
