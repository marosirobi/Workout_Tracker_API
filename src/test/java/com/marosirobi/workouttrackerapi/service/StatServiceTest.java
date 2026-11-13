package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSessionStat;
import com.marosirobi.workouttrackerapi.dto.stat.ExerciseWorkoutBest;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.repository.BestEffortRepository;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.ExerciseSetRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StatServiceTest {

    private static final String EMAIL = "user@gmail.com";
    private static final double METERS_PER_DEGREE_LAT = 111_194.93;
    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");

    @Mock private ExerciseSetRepository exerciseSetRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private BestEffortRepository bestEffortRepository;

    @InjectMocks
    private StatService statService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email(EMAIL).build();
    }

    private static TrackPoint point(double meters, double seconds, int segment) {
        return TrackPoint.builder()
                .latitude(47.0 + meters / METERS_PER_DEGREE_LAT)
                .longitude(19.0)
                .elevation(100.0)
                .speed(0.0)
                .segment(segment)
                .timestamp(START.plusMillis(Math.round(seconds * 1000)))
                .build();
    }

    // points every stepMeters at constant speed
    private static List<TrackPoint> run(double fromMeters, double toMeters, double stepMeters,
                                        double startSeconds, double speedMs, int segment) {
        List<TrackPoint> points = new ArrayList<>();
        for (double m = fromMeters; m <= toMeters + 0.001; m += stepMeters) {
            points.add(point(m, startSeconds + (m - fromMeters) / speedMs, segment));
        }
        return points;
    }

    private static double durationFor(List<BestEffort> efforts, int distance) {
        return efforts.stream()
                .filter(e -> e.getDistanceMeters() == distance)
                .findFirst()
                .orElseThrow()
                .getDurationSeconds();
    }

    @Nested
    class BestEfforts {

        private final Workout workout = Workout.builder().id(10L).type(WorkoutType.CARDIO).build();

        @Test
        void tooFewPoints_givesNoEfforts() {
            assertThat(statService.calculate(workout, null)).isEmpty();
            assertThat(statService.calculate(workout, List.of(point(0, 0, 0)))).isEmpty();
        }

        @Test
        void constantPace_givesEffortsUpToTotalDistance() {
            var efforts = statService.calculate(workout, run(0, 1200, 100, 0, 4.0, 0));

            assertThat(efforts).extracting(BestEffort::getDistanceMeters).containsExactly(400, 1000);
            assertThat(durationFor(efforts, 400)).isCloseTo(100.0, within(0.5));
            assertThat(durationFor(efforts, 1000)).isCloseTo(250.0, within(0.5));
            assertThat(efforts).allSatisfy(e -> assertThat(e.getWorkout()).isSameAs(workout));
        }

        @Test
        void unorderedPoints_areSortedByTime() {
            var points = new ArrayList<>(run(0, 1200, 100, 0, 4.0, 0));
            Collections.reverse(points);

            assertThat(durationFor(statService.calculate(workout, points), 1000)).isCloseTo(250.0, within(0.5));
        }

        @Test
        void findsFastestWindow() {
            var points = new ArrayList<>(run(0, 400, 100, 0, 2.0, 0));
            points.addAll(run(500, 1000, 100, 250, 5.0, 0));

            var efforts = statService.calculate(workout, points);

            assertThat(durationFor(efforts, 400)).isCloseTo(80.0, within(0.5));
            assertThat(efforts).filteredOn(e -> e.getDistanceMeters() == 400).singleElement()
                    .satisfies(e -> assertThat(e.getStartOffsetMeters()).isBetween(499.0, 601.0));
        }

        @Test
        void pauseBetweenSegments_isIgnored() {
            var points = new ArrayList<>(run(0, 600, 100, 0, 4.0, 0));
            points.addAll(run(700, 1200, 100, 900, 4.0, 1));

            var efforts = statService.calculate(workout, points);

            assertThat(durationFor(efforts, 1000)).isCloseTo(250.0, within(0.5));
        }

        @Test
        void unrealisticSpeed_isIgnored() {
            var points = List.of(point(0, 0, 0), point(1000, 10, 0));

            assertThat(statService.calculate(workout, points)).isEmpty();
        }

        @Test
        void calculateAndSave_persistsAndAttachesEfforts() {
            var cardio = Workout.builder().id(11L).type(WorkoutType.CARDIO).build();

            statService.calculateAndSave(cardio, run(0, 1200, 100, 0, 4.0, 0));

            assertThat(cardio.getBestEfforts()).extracting(BestEffort::getDistanceMeters).containsExactly(400, 1000);
            then(bestEffortRepository).should().saveAll(cardio.getBestEfforts());
        }
    }

    @Nested
    class WeightRanks {

        private Workout finishedWorkoutWith(Exercise exercise) {
            var workout = Workout.builder()
                    .id(10L)
                    .user(user)
                    .type(WorkoutType.STRENGTH)
                    .startTime(START)
                    .endTime(START.plus(Duration.ofHours(1)))
                    .build();
            workout.getExercises().add(WorkoutExercise.builder().exercise(exercise).orderIndex(0).build());
            return workout;
        }

        private ExerciseWorkoutBest row(long workoutId, Instant start, Double weight, Integer reps) {
            return new ExerciseWorkoutBest(1L, workoutId, start, weight, reps, null);
        }

        @Test
        void unfinishedWorkout_hasNoRanks() {
            var workout = finishedWorkoutWith(Exercise.builder().id(1L).build());
            workout.setEndTime(null);

            assertThat(statService.computeWeightRanks(workout)).isEmpty();
            then(exerciseSetRepository).shouldHaveNoInteractions();
        }

        @Test
        void weightExercise_isRankedByMaxWeight() {
            var bench = Exercise.builder().id(1L).trackingType(TrackingType.WEIGHT_REPS).build();
            given(exerciseSetRepository.findBestPerWorkout(eq(1L), anyCollection())).willReturn(List.of(
                    row(10L, START, 100.0, 5),
                    row(9L, START.minus(Duration.ofDays(7)), 110.0, 3),
                    row(8L, START.minus(Duration.ofDays(14)), 90.0, 8)));

            assertThat(statService.computeWeightRanks(finishedWorkoutWith(bench))).containsEntry(1L, 2);
        }

        @Test
        void tie_earlierWorkoutWins() {
            var bench = Exercise.builder().id(1L).trackingType(TrackingType.WEIGHT_REPS).build();
            given(exerciseSetRepository.findBestPerWorkout(eq(1L), anyCollection())).willReturn(List.of(
                    row(10L, START, 100.0, 5),
                    row(9L, START.minus(Duration.ofDays(7)), 100.0, 5),
                    row(11L, START.plus(Duration.ofDays(7)), 100.0, 5)));

            assertThat(statService.computeWeightRanks(finishedWorkoutWith(bench))).containsEntry(1L, 2);
        }

        @Test
        void bodyweightExercise_isRankedByReps() {
            var pullUp = Exercise.builder().id(1L).trackingType(TrackingType.BODYWEIGHT).build();
            given(exerciseSetRepository.findBestPerWorkout(eq(1L), anyCollection())).willReturn(List.of(
                    row(10L, START, 0.0, 12),
                    row(9L, START.minus(Duration.ofDays(7)), 0.0, 10)));

            assertThat(statService.computeWeightRanks(finishedWorkoutWith(pullUp))).containsEntry(1L, 1);
        }

        @Test
        void zeroValue_isNotRanked() {
            var bench = Exercise.builder().id(1L).trackingType(TrackingType.WEIGHT_REPS).build();
            given(exerciseSetRepository.findBestPerWorkout(eq(1L), anyCollection()))
                    .willReturn(List.of(row(10L, START, 0.0, 5)));

            assertThat(statService.computeWeightRanks(finishedWorkoutWith(bench))).isEmpty();
        }
    }

    @Nested
    class ExerciseStats {

        private ExerciseSessionStat session(long workoutId, Double maxWeight, Double oneRepMax, Double setVolume,
                                            Double sessionVolume, Integer reps, Integer duration) {
            return new ExerciseSessionStat(workoutId, START.plus(Duration.ofDays(workoutId)),
                    maxWeight, oneRepMax, setVolume, sessionVolume, reps, duration);
        }

        private void givenExercise(TrackingType type) {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(exerciseRepository.findById(1L)).willReturn(Optional.of(
                    Exercise.builder().id(1L).exerciseName("Test").muscleGroup(MuscleGroup.CHEST).trackingType(type).build()));
        }

        @Test
        void weightExercise_hasWeightRecords() {
            givenExercise(TrackingType.WEIGHT_REPS);
            given(exerciseSetRepository.findSessionStats(1L, 1L, 0.0)).willReturn(List.of(
                    session(1, 100.0, 116.66, 500.0, 830.0, 5, null),
                    session(2, 120.0, 121.04, 360.0, 360.0, 3, null)));

            var stats = statService.getExerciseStats(1L, EMAIL);

            assertThat(stats.heaviestWeight()).returns(120.0, r -> r.value()).returns(2L, r -> r.workoutId());
            assertThat(stats.bestOneRepMax().value()).isEqualTo(121.0);
            assertThat(stats.bestSetVolume()).returns(500.0, r -> r.value()).returns(1L, r -> r.workoutId());
            assertThat(stats.bestSessionVolume().value()).isEqualTo(830.0);
            assertThat(stats.mostReps()).isNull();
            assertThat(stats.longestDuration()).isNull();
            assertThat(stats.history()).hasSize(2);
        }

        @Test
        void bodyweightExercise_usesBodyWeightFactor() {
            givenExercise(TrackingType.BODYWEIGHT);
            given(exerciseSetRepository.findSessionStats(1L, 1L, 1.0)).willReturn(List.of(
                    session(1, 0.0, 100.0, 960.0, 1760.0, 12, null)));

            var stats = statService.getExerciseStats(1L, EMAIL);

            assertThat(stats.heaviestWeight()).isNull();
            assertThat(stats.bestOneRepMax()).isNull();
            assertThat(stats.mostReps().value()).isEqualTo(12.0);
            assertThat(stats.bestSetVolume().value()).isEqualTo(960.0);
        }

        @Test
        void durationExercise_hasOnlyDurationRecord() {
            givenExercise(TrackingType.DURATION);
            given(exerciseSetRepository.findSessionStats(1L, 1L, 0.0)).willReturn(List.of(
                    session(1, 0.0, 0.0, 0.0, 0.0, 0, 90)));

            var stats = statService.getExerciseStats(1L, EMAIL);

            assertThat(stats.longestDuration().value()).isEqualTo(90.0);
            assertThat(stats.heaviestWeight()).isNull();
            assertThat(stats.bestSetVolume()).isNull();
            assertThat(stats.mostReps()).isNull();
        }

        @Test
        void noHistory_givesNoRecords() {
            givenExercise(TrackingType.WEIGHT_REPS);
            given(exerciseSetRepository.findSessionStats(1L, 1L, 0.0)).willReturn(List.of());

            var stats = statService.getExerciseStats(1L, EMAIL);

            assertThat(stats.heaviestWeight()).isNull();
            assertThat(stats.bestOneRepMax()).isNull();
            assertThat(stats.history()).isEmpty();
        }

        @Test
        void unknownExercise_throws() {
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
            given(exerciseRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> statService.getExerciseStats(99L, EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
