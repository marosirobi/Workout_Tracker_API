package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.dto.template.*;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.TemplateExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import com.marosirobi.workouttrackerapi.repository.WorkoutTemplateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;

    @Transactional
    public WorkoutTemplateResponse createTemplate(CreateWorkoutTemplateRequest request, String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        WorkoutTemplate workoutTemplate = WorkoutTemplate.builder()
                .user(user)
                .templateName(request.templateName())
                .build();

        if(request.exercises() != null){
            for(TemplateExerciseRequest exReq : request.exercises()){
                Exercise exercise = exerciseRepository.findById(exReq.exerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found " + exReq.exerciseId()));

                TemplateExercise templateExercise = TemplateExercise.builder()
                        .exercise(exercise)
                        .orderIndex(exReq.orderIndex())
                        .targetSets(exReq.targetSets())
                        .workoutTemplate(workoutTemplate)
                        .build();

                workoutTemplate.getExercises().add(templateExercise);
            }
        }

        WorkoutTemplate savedTemplate = workoutTemplateRepository.save(workoutTemplate);
        return mapToResponse(savedTemplate);
    }

    public List<WorkoutTemplateResponse> getMyTemplates(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        return workoutTemplateRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public WorkoutTemplateResponse updateTemplate(Long id, UpdateWorkoutTemplateRequest request, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        WorkoutTemplate workoutTemplate = workoutTemplateRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout template not found " + id));

        workoutTemplate.setTemplateName(request.templateName());

        if(workoutTemplate.getExercises() != null){
            workoutTemplate.getExercises().clear();
        }

        if(request.exercises() != null && !request.exercises().isEmpty()){
            List<TemplateExercise> updatedExercises = request.exercises().stream().map(dto ->{
                Exercise exercise = exerciseRepository.findById(dto.exerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found " + dto.exerciseId()));

                return TemplateExercise.builder()
                        .exercise(exercise)
                        .orderIndex(dto.orderIndex())
                        .targetSets(dto.targetSets())
                        .workoutTemplate(workoutTemplate)
                        .build();
            })
                    .toList();

            workoutTemplate.getExercises().addAll(updatedExercises);
        }
        workoutTemplateRepository.save(workoutTemplate);
        return mapToResponse(workoutTemplate);
    }

    @Transactional
    public void deleteTemplate(Long id, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        if(!workoutTemplateRepository.existsByIdAndUserId(id, user.getId())){
            throw new RuntimeException("Workout template doesn't exist or you don't have permission: " + id);
        }
        workoutTemplateRepository.deleteById(id);
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
