package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import com.marosirobi.workouttrackerapi.domain.entity.TemplateExercise;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.domain.entity.WorkoutTemplate;
import com.marosirobi.workouttrackerapi.dto.template.TemplateExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateRequest;
import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateResponse;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.mapper.WorkoutTemplateMapper;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import com.marosirobi.workouttrackerapi.repository.WorkoutTemplateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutTemplateMapper workoutTemplateMapper;

    @Transactional
    public WorkoutTemplateResponse createTemplate(WorkoutTemplateRequest request, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        WorkoutTemplate workoutTemplate = WorkoutTemplate.builder()
                .user(user)
                .templateName(request.templateName())
                .build();

        replaceExercises(workoutTemplate, request.exercises());

        WorkoutTemplate savedTemplate = workoutTemplateRepository.save(workoutTemplate);
        log.info("Template created, template={}, user={}", savedTemplate.getId(), user.getId());
        return workoutTemplateMapper.mapToResponse(savedTemplate);
    }

    public List<WorkoutTemplateResponse> getMyTemplates(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        return workoutTemplateRepository.findByUserId(user.getId())
                .stream()
                .map(workoutTemplateMapper::mapToResponse)
                .toList();
    }

    @Transactional
    public WorkoutTemplateResponse updateTemplate(Long id, WorkoutTemplateRequest request, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        WorkoutTemplate workoutTemplate = workoutTemplateRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Workout template not found " + id));

        workoutTemplate.setTemplateName(request.templateName());
        replaceExercises(workoutTemplate, request.exercises());

        workoutTemplateRepository.save(workoutTemplate);
        log.info("Template updated, template={}, user={}", id, user.getId());
        return workoutTemplateMapper.mapToResponse(workoutTemplate);
    }

    @Transactional
    public void deleteTemplate(Long id, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        if(!workoutTemplateRepository.existsByIdAndUserId(id, user.getId())){
            throw new ResourceNotFoundException("Workout template doesn't exist or you don't have permission: " + id);
        }
        workoutTemplateRepository.deleteById(id);
        log.info("Template deleted, template={}, user={}", id, user.getId());
    }

    private void replaceExercises(WorkoutTemplate template, List<TemplateExerciseRequest> requests) {
        template.getExercises().clear();
        if (requests == null) return;

        for (TemplateExerciseRequest exReq : requests) {
            Exercise exercise = exerciseRepository.findById(exReq.exerciseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exercise not found " + exReq.exerciseId()));

            TemplateExercise templateExercise = TemplateExercise.builder()
                    .exercise(exercise)
                    .orderIndex(exReq.orderIndex())
                    .targetSets(exReq.targetSets())
                    .workoutTemplate(template)
                    .build();

            template.getExercises().add(templateExercise);
        }
    }
}
