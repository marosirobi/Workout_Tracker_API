package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import com.marosirobi.workouttrackerapi.domain.entity.TemplateExercise;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.domain.entity.WorkoutTemplate;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.dto.template.TemplateExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateRequest;
import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateResponse;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.mapper.WorkoutTemplateMapper;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import com.marosirobi.workouttrackerapi.repository.WorkoutTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutTemplateServiceTest {

    private static final String EMAIL = "user@gmail.com";

    @Mock private WorkoutTemplateRepository workoutTemplateRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private WorkoutTemplateMapper workoutTemplateMapper;

    @InjectMocks
    private WorkoutTemplateService workoutTemplateService;

    private User user;
    private Exercise bench;
    private Exercise squat;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email(EMAIL).build();
        bench = Exercise.builder().id(1L).exerciseName("Bench press").muscleGroup(MuscleGroup.CHEST).build();
        squat = Exercise.builder().id(2L).exerciseName("Squat").muscleGroup(MuscleGroup.LEGS).build();
    }

    private void givenUser() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
    }

    private WorkoutTemplate existingTemplate() {
        var template = WorkoutTemplate.builder().id(5L).user(user).templateName("Old").build();
        template.getExercises().add(TemplateExercise.builder().exercise(bench).orderIndex(0).targetSets(3).workoutTemplate(template).build());
        return template;
    }

    @Nested
    class Create {

        @Test
        void savesTemplateWithExercises() {
            givenUser();
            given(exerciseRepository.findById(1L)).willReturn(Optional.of(bench));
            given(exerciseRepository.findById(2L)).willReturn(Optional.of(squat));
            given(workoutTemplateRepository.save(any(WorkoutTemplate.class))).willAnswer(inv -> inv.getArgument(0));

            workoutTemplateService.createTemplate(new WorkoutTemplateRequest("Full body", List.of(
                    new TemplateExerciseRequest(1L, 0, 3),
                    new TemplateExerciseRequest(2L, 1, 5))), EMAIL);

            var saved = ArgumentCaptor.forClass(WorkoutTemplate.class);
            then(workoutTemplateRepository).should().save(saved.capture());
            assertThat(saved.getValue())
                    .returns("Full body", WorkoutTemplate::getTemplateName)
                    .returns(user, WorkoutTemplate::getUser);
            assertThat(saved.getValue().getExercises())
                    .extracting(te -> te.getExercise().getId(), TemplateExercise::getOrderIndex, TemplateExercise::getTargetSets)
                    .containsExactly(tuple(1L, 0, 3), tuple(2L, 1, 5));
            assertThat(saved.getValue().getExercises())
                    .allSatisfy(te -> assertThat(te.getWorkoutTemplate()).isSameAs(saved.getValue()));
        }

        @Test
        void rejectsUnknownExercise() {
            givenUser();
            given(exerciseRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> workoutTemplateService.createTemplate(
                    new WorkoutTemplateRequest("Broken", List.of(new TemplateExerciseRequest(99L, 0, 3))), EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
            then(workoutTemplateRepository).should(never()).save(any());
        }

        @Test
        void allowsEmptyExerciseList() {
            givenUser();
            given(workoutTemplateRepository.save(any(WorkoutTemplate.class))).willAnswer(inv -> inv.getArgument(0));

            workoutTemplateService.createTemplate(new WorkoutTemplateRequest("Empty", null), EMAIL);

            var saved = ArgumentCaptor.forClass(WorkoutTemplate.class);
            then(workoutTemplateRepository).should().save(saved.capture());
            assertThat(saved.getValue().getExercises()).isEmpty();
        }
    }

    @Nested
    class Update {

        @Test
        void replacesNameAndExercises() {
            var template = existingTemplate();
            givenUser();
            given(workoutTemplateRepository.findByIdAndUserId(5L, 1L)).willReturn(Optional.of(template));
            given(exerciseRepository.findById(2L)).willReturn(Optional.of(squat));

            workoutTemplateService.updateTemplate(5L,
                    new WorkoutTemplateRequest("Leg day", List.of(new TemplateExerciseRequest(2L, 0, 4))), EMAIL);

            assertThat(template.getTemplateName()).isEqualTo("Leg day");
            assertThat(template.getExercises()).singleElement()
                    .returns(squat, TemplateExercise::getExercise)
                    .returns(4, TemplateExercise::getTargetSets);
            then(workoutTemplateRepository).should().save(template);
        }

        @Test
        void someoneElsesTemplate_isNotFound() {
            givenUser();
            given(workoutTemplateRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> workoutTemplateService.updateTemplate(99L,
                    new WorkoutTemplateRequest("x", List.of()), EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
            then(workoutTemplateRepository).should(never()).save(any());
        }
    }

    @Nested
    class Delete {

        @Test
        void ownTemplate_isDeleted() {
            givenUser();
            given(workoutTemplateRepository.existsByIdAndUserId(5L, 1L)).willReturn(true);

            workoutTemplateService.deleteTemplate(5L, EMAIL);

            then(workoutTemplateRepository).should().deleteById(5L);
        }

        @Test
        void someoneElsesTemplate_isNotFound() {
            givenUser();
            given(workoutTemplateRepository.existsByIdAndUserId(99L, 1L)).willReturn(false);

            assertThatThrownBy(() -> workoutTemplateService.deleteTemplate(99L, EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
            then(workoutTemplateRepository).should(never()).deleteById(any());
        }
    }

    @Test
    void listsOwnTemplates() {
        var template = existingTemplate();
        var response = WorkoutTemplateResponse.builder().id(5L).templateName("Old").build();
        givenUser();
        given(workoutTemplateRepository.findByUserId(1L)).willReturn(List.of(template));
        given(workoutTemplateMapper.mapToResponse(template)).willReturn(response);

        assertThat(workoutTemplateService.getMyTemplates(EMAIL)).containsExactly(response);
    }
}
