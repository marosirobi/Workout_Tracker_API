package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioDataRequest;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioStatsResponse;
import com.marosirobi.workouttrackerapi.dto.cardio.TrackPointResponse;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSetRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.workout.CreateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.EditWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.UpdateWorkoutRequest;
import com.marosirobi.workouttrackerapi.exception.BadRequestException;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.exception.UnauthorizedAccessException;
import com.marosirobi.workouttrackerapi.mapper.WorkoutMapper;
import com.marosirobi.workouttrackerapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    private static final String EMAIL = "user@gmail.com";

    @Mock private WorkoutRepository workoutRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkoutTemplateRepository workoutTemplateRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private TrackPointRepository trackPointRepository;
    @Mock private CardioStatsRepository cardioStatsRepository;
    @Mock private StatService statService;
    @Mock private WorkoutMapper workoutMapper;

    @InjectMocks
    private WorkoutService workoutService;

    @Captor
    private ArgumentCaptor<List<TrackPoint>> pointsCaptor;

    private User user;
    private Exercise bench;
    private Exercise pullUp;
    private Exercise plank;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email(EMAIL).bodyWeightKg(80.0).build();
        bench = exercise(1L, "Bench press", TrackingType.WEIGHT_REPS);
        pullUp = exercise(2L, "Pull-up", TrackingType.BODYWEIGHT);
        plank = exercise(3L, "Plank", TrackingType.DURATION);
    }

    private static Exercise exercise(Long id, String name, TrackingType type) {
        return Exercise.builder().id(id).exerciseName(name).muscleGroup(MuscleGroup.CHEST).trackingType(type).build();
    }

    private Workout workout(WorkoutType type, boolean finished) {
        Instant start = Instant.parse("2026-11-01T10:00:00Z");
        return Workout.builder()
                .id(10L)
                .user(user)
                .type(type)
                .bodyWeightKg(80.0)
                .startTime(start)
                .endTime(finished ? start.plus(Duration.ofMinutes(45)) : null)
                .build();
    }

    private static WorkoutExercise workoutExercise(Exercise exercise, ExerciseSet... sets) {
        var workoutExercise = WorkoutExercise.builder().exercise(exercise).orderIndex(0).build();
        workoutExercise.getSets().addAll(List.of(sets));
        return workoutExercise;
    }

    private static ExerciseSet set(double weight, int reps) {
        return ExerciseSet.builder().setNumber(1).weight(weight).reps(reps).build();
    }

    private void givenUser() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
    }

    private void givenOwnWorkout(Workout workout) {
        given(workoutRepository.findByIdAndUserId(workout.getId(), user.getId())).willReturn(Optional.of(workout));
    }

    @Nested
    class CreateWorkout {

        @Test
        void requiresBodyWeight() {
            user.setBodyWeightKg(null);
            givenUser();

            assertThatThrownBy(() -> workoutService.createWorkout(
                    new CreateWorkoutRequest(WorkoutType.STRENGTH, Instant.now(), null), EMAIL))
                    .isInstanceOf(BadRequestException.class);
            then(workoutRepository).should(never()).save(any());
        }

        @Test
        void rejectsTemplateForCardio() {
            givenUser();

            assertThatThrownBy(() -> workoutService.createWorkout(
                    new CreateWorkoutRequest(WorkoutType.CARDIO, Instant.now(), 5L), EMAIL))
                    .isInstanceOf(BadRequestException.class);
            then(workoutTemplateRepository).shouldHaveNoInteractions();
        }

        @Test
        void storesBodyWeightSnapshot() {
            givenUser();
            given(workoutRepository.save(any(Workout.class))).willAnswer(inv -> inv.getArgument(0));

            workoutService.createWorkout(new CreateWorkoutRequest(WorkoutType.STRENGTH, Instant.now(), null), EMAIL);

            var saved = ArgumentCaptor.forClass(Workout.class);
            then(workoutRepository).should().save(saved.capture());
            assertThat(saved.getValue())
                    .returns(80.0, Workout::getBodyWeightKg)
                    .returns(WorkoutType.STRENGTH, Workout::getType)
                    .returns(user, Workout::getUser);
        }

        @Test
        void fromTemplate_createsEmptySets() {
            var template = WorkoutTemplate.builder().id(5L).user(user).templateName("Push").build();
            template.getExercises().add(TemplateExercise.builder().exercise(bench).orderIndex(0).targetSets(3).build());
            givenUser();
            given(workoutTemplateRepository.findByIdAndUserId(5L, 1L)).willReturn(Optional.of(template));
            given(workoutRepository.save(any(Workout.class))).willAnswer(inv -> inv.getArgument(0));

            workoutService.createWorkout(new CreateWorkoutRequest(WorkoutType.STRENGTH, Instant.now(), 5L), EMAIL);

            var saved = ArgumentCaptor.forClass(Workout.class);
            then(workoutRepository).should().save(saved.capture());
            assertThat(saved.getValue().getTemplate()).isSameAs(template);
            assertThat(saved.getValue().getExercises()).singleElement()
                    .satisfies(we -> assertThat(we.getSets())
                            .extracting(ExerciseSet::getSetNumber, ExerciseSet::getWeight, ExerciseSet::getReps)
                            .containsExactly(tuple(1, 0.0, 0), tuple(2, 0.0, 0), tuple(3, 0.0, 0)));
        }

        @Test
        void rejectsSomeoneElsesTemplate() {
            givenUser();
            given(workoutTemplateRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> workoutService.createWorkout(
                    new CreateWorkoutRequest(WorkoutType.STRENGTH, Instant.now(), 99L), EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class FinishWorkout {

        private UpdateWorkoutRequest finish(WorkoutExerciseRequest... exercises) {
            return new UpdateWorkoutRequest("Push day", null, Instant.now(), List.of(exercises));
        }

        @Test
        void buildsSetsByTrackingType() {
            var workout = workout(WorkoutType.STRENGTH, false);
            givenUser();
            givenOwnWorkout(workout);
            given(exerciseRepository.findById(1L)).willReturn(Optional.of(bench));
            given(exerciseRepository.findById(2L)).willReturn(Optional.of(pullUp));
            given(exerciseRepository.findById(3L)).willReturn(Optional.of(plank));
            given(workoutRepository.save(workout)).willReturn(workout);

            workoutService.updateWorkout(10L, finish(
                    new WorkoutExerciseRequest(1L, 0, List.of(new ExerciseSetRequest(1, 100.0, null, 5))),
                    new WorkoutExerciseRequest(2L, 1, List.of(new ExerciseSetRequest(1, 20.0, null, 10))),
                    new WorkoutExerciseRequest(3L, 2, List.of(new ExerciseSetRequest(1, 5.0, 60, 3)))
            ), EMAIL);

            assertThat(workout.getEndTime()).isNotNull();
            assertThat(workout.getWorkoutTitle()).isEqualTo("Push day");
            assertThat(workout.getExercises())
                    .extracting(we -> we.getSets().getFirst())
                    .extracting(ExerciseSet::getWeight, ExerciseSet::getReps, ExerciseSet::getDurationSeconds)
                    .containsExactly(
                            tuple(100.0, 5, null),
                            tuple(0.0, 10, null),
                            tuple(0.0, 0, 60));
        }

        @Test
        void durationExercise_requiresDuration() {
            var workout = workout(WorkoutType.STRENGTH, false);
            givenUser();
            givenOwnWorkout(workout);
            given(exerciseRepository.findById(3L)).willReturn(Optional.of(plank));

            assertThatThrownBy(() -> workoutService.updateWorkout(10L, finish(
                    new WorkoutExerciseRequest(3L, 0, List.of(new ExerciseSetRequest(1, 0.0, null, 0)))
            ), EMAIL)).isInstanceOf(BadRequestException.class);
        }

        @Test
        void rejectsExerciseChangesAfterEnd() {
            givenUser();
            givenOwnWorkout(workout(WorkoutType.STRENGTH, true));

            assertThatThrownBy(() -> workoutService.updateWorkout(10L, finish(
                    new WorkoutExerciseRequest(1L, 0, List.of(new ExerciseSetRequest(1, 100.0, null, 5)))
            ), EMAIL)).isInstanceOf(BadRequestException.class);
            then(workoutRepository).should(never()).save(any());
        }

        @Test
        void rejectsUnknownExercise() {
            givenUser();
            givenOwnWorkout(workout(WorkoutType.STRENGTH, false));
            given(exerciseRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> workoutService.updateWorkout(10L, finish(
                    new WorkoutExerciseRequest(99L, 0, List.of(new ExerciseSetRequest(1, 100.0, null, 5)))
            ), EMAIL)).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class EditWorkout {

        @Test
        void requiresFinishedWorkout() {
            givenUser();
            givenOwnWorkout(workout(WorkoutType.STRENGTH, false));

            assertThatThrownBy(() -> workoutService.editWorkout(
                    10L, EditWorkoutRequest.builder().workoutTitle("New title").build(), EMAIL))
                    .isInstanceOf(BadRequestException.class);
            then(workoutRepository).should(never()).save(any());
        }

        @Test
        void strengthWorkout_needsAtLeastOneExercise() {
            givenUser();
            givenOwnWorkout(workout(WorkoutType.STRENGTH, true));

            assertThatThrownBy(() -> workoutService.editWorkout(
                    10L, EditWorkoutRequest.builder().exercises(List.of()).build(), EMAIL))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void cardioWorkout_cannotHaveExercises() {
            givenUser();
            givenOwnWorkout(workout(WorkoutType.CARDIO, true));

            assertThatThrownBy(() -> workoutService.editWorkout(
                    10L, EditWorkoutRequest.builder().exercises(List.of()).build(), EMAIL))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void trimsTitle_andClearsBlankNote() {
            var workout = workout(WorkoutType.STRENGTH, true);
            workout.setWorkoutNote("old note");
            givenUser();
            givenOwnWorkout(workout);
            given(workoutRepository.save(workout)).willReturn(workout);

            workoutService.editWorkout(10L,
                    EditWorkoutRequest.builder().workoutTitle("  Leg day  ").workoutNote("   ").build(), EMAIL);

            assertThat(workout.getWorkoutTitle()).isEqualTo("Leg day");
            assertThat(workout.getWorkoutNote()).isNull();
        }

        @Test
        void someoneElsesWorkout_isNotFound() {
            givenUser();
            given(workoutRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> workoutService.editWorkout(
                    99L, EditWorkoutRequest.builder().workoutTitle("x").build(), EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class SaveCardioData {

        private CardioDataRequest request(List<TrackPointResponse> points) {
            return CardioDataRequest.builder()
                    .workoutTitle("Morning run")
                    .endTime(Instant.parse("2026-11-01T10:30:00Z"))
                    .stats(new CardioStatsResponse(5.2, "5:45", 30, 1800))
                    .trackPoints(points)
                    .build();
        }

        @Test
        void savesStatsPoints_andBestEfforts() {
            var workout = workout(WorkoutType.CARDIO, false);
            givenUser();
            given(workoutRepository.findById(10L)).willReturn(Optional.of(workout));
            var point = new TrackPointResponse(47.5, 19.0, 100.0, 3.0, null, Instant.parse("2026-11-01T10:00:05Z"));

            workoutService.saveCardioData(10L, request(List.of(point, point)), EMAIL);

            var stats = ArgumentCaptor.forClass(CardioStats.class);
            then(cardioStatsRepository).should().save(stats.capture());
            assertThat(stats.getValue())
                    .returns(5.2, CardioStats::getTotalDistance)
                    .returns("5:45", CardioStats::getAvgPace)
                    .returns(1800, CardioStats::getMovingTimeSeconds);
            then(statService).should().calculateAndSave(eq(workout), pointsCaptor.capture());
            assertThat(pointsCaptor.getValue()).hasSize(2).allSatisfy(p -> assertThat(p.getSegment()).isZero());
            then(trackPointRepository).should().saveAll(pointsCaptor.getValue());
            assertThat(workout.getWorkoutTitle()).isEqualTo("Morning run");
            assertThat(workout.getEndTime()).isEqualTo(Instant.parse("2026-11-01T10:30:00Z"));
        }

        @Test
        void withoutPoints_skipsBestEfforts() {
            givenUser();
            given(workoutRepository.findById(10L)).willReturn(Optional.of(workout(WorkoutType.CARDIO, false)));

            workoutService.saveCardioData(10L, request(List.of()), EMAIL);

            then(trackPointRepository).shouldHaveNoInteractions();
            then(statService).shouldHaveNoInteractions();
        }

        @Test
        void rejectsSomeoneElsesWorkout() {
            var other = workout(WorkoutType.CARDIO, false);
            other.setUser(User.builder().id(2L).build());
            givenUser();
            given(workoutRepository.findById(10L)).willReturn(Optional.of(other));

            assertThatThrownBy(() -> workoutService.saveCardioData(10L, request(List.of()), EMAIL))
                    .isInstanceOf(UnauthorizedAccessException.class);
            then(cardioStatsRepository).shouldHaveNoInteractions();
        }

        @Test
        void rejectsStrengthWorkout() {
            givenUser();
            given(workoutRepository.findById(10L)).willReturn(Optional.of(workout(WorkoutType.STRENGTH, false)));

            assertThatThrownBy(() -> workoutService.saveCardioData(10L, request(List.of()), EMAIL))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void rejectsFinishedWorkout() {
            givenUser();
            given(workoutRepository.findById(10L)).willReturn(Optional.of(workout(WorkoutType.CARDIO, true)));

            assertThatThrownBy(() -> workoutService.saveCardioData(10L, request(List.of()), EMAIL))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    class Queries {

        @Test
        void list_withoutType_returnsFinishedWorkoutsNewestFirst() {
            var strength = workout(WorkoutType.STRENGTH, true);
            strength.getExercises().add(workoutExercise(bench, set(100, 5)));
            strength.getExercises().add(workoutExercise(pullUp, set(0, 10), set(0, 8)));
            givenUser();
            given(workoutRepository.findByUserIdAndEndTimeIsNotNull(eq(1L), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(strength)));

            var page = workoutService.getUserWorkouts(EMAIL, 0, 10, null);

            var pageable = ArgumentCaptor.forClass(Pageable.class);
            then(workoutRepository).should().findByUserIdAndEndTimeIsNotNull(eq(1L), pageable.capture());
            assertThat(pageable.getValue().getSort().getOrderFor("startTime"))
                    .returns(Sort.Direction.DESC, Sort.Order::getDirection);
            assertThat(page.getContent()).singleElement().satisfies(summary -> {
                assertThat(summary.totalVolume()).isEqualTo(500.0 + 80 * 10 + 80 * 8);
                assertThat(summary.totalSets()).isEqualTo(3);
                assertThat(summary.exerciseCount()).isEqualTo(2);
                assertThat(summary.durationSeconds()).isEqualTo(45L * 60);
            });
        }

        @Test
        void list_withType_usesTypeFilter_andCardioStats() {
            var cardio = workout(WorkoutType.CARDIO, true);
            cardio.setCardioStats(CardioStats.builder().totalDistance(5.0).avgPace("6:00").movingTimeSeconds(1800).build());
            givenUser();
            given(workoutRepository.findByUserIdAndTypeAndEndTimeIsNotNull(eq(1L), any(Pageable.class), eq(WorkoutType.CARDIO)))
                    .willReturn(new PageImpl<>(List.of(cardio)));

            var page = workoutService.getUserWorkouts(EMAIL, 0, 10, WorkoutType.CARDIO);

            assertThat(page.getContent()).singleElement().satisfies(summary -> {
                assertThat(summary.totalDistance()).isEqualTo(5.0);
                assertThat(summary.avgPace()).isEqualTo("6:00");
                assertThat(summary.totalVolume()).isNull();
            });
            then(workoutRepository).should(never()).findByUserIdAndEndTimeIsNotNull(any(), any());
        }

        @Test
        void range_rejectsReversedInterval() {
            Instant now = Instant.now();

            assertThatThrownBy(() -> workoutService.getWorkoutsInRange(now, now.minusSeconds(1), EMAIL))
                    .isInstanceOf(BadRequestException.class);
            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        void range_rejectsTooLongInterval() {
            Instant now = Instant.now();

            assertThatThrownBy(() -> workoutService.getWorkoutsInRange(now.minus(Duration.ofDays(401)), now, EMAIL))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void range_mapsWorkouts() {
            Instant to = Instant.parse("2026-11-30T00:00:00Z");
            Instant from = to.minus(Duration.ofDays(30));
            givenUser();
            given(workoutRepository.findByUserIdAndEndTimeIsNotNullAndStartTimeBetweenOrderByStartTimeAsc(1L, from, to))
                    .willReturn(List.of(workout(WorkoutType.CARDIO, true)));

            var result = workoutService.getWorkoutsInRange(from, to, EMAIL);

            assertThat(result).singleElement()
                    .satisfies(item -> assertThat(item.type()).isEqualTo(WorkoutType.CARDIO));
        }

        @Test
        void byId_someoneElsesWorkout_isNotFound() {
            givenUser();
            given(workoutRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> workoutService.getWorkoutById(99L, EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
            then(workoutMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    class DeleteWorkout {

        @Test
        void someoneElsesWorkout_isNotFound() {
            givenUser();
            given(workoutRepository.existsByIdAndUserId(99L, 1L)).willReturn(false);

            assertThatThrownBy(() -> workoutService.deleteWorkout(99L, EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
            then(workoutRepository).should(never()).deleteById(any());
        }

        @Test
        void ownWorkout_isDeleted() {
            givenUser();
            given(workoutRepository.existsByIdAndUserId(10L, 1L)).willReturn(true);

            workoutService.deleteWorkout(10L, EMAIL);

            then(workoutRepository).should().deleteById(10L);
        }
    }
}
