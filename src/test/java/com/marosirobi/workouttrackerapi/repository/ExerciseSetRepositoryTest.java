package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSessionStat;
import com.marosirobi.workouttrackerapi.dto.stat.ExerciseWorkoutBest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ExerciseSetRepositoryTest {

    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");

    @Autowired private ExerciseSetRepository exerciseSetRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private WorkoutRepository workoutRepository;

    private User user;
    private User otherUser;
    private Exercise bench;
    private Exercise pullUp;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder().username("user").email("user@test.hu").password("x").build());
        otherUser = userRepository.save(User.builder().username("other").email("other@test.hu").password("x").build());
        bench = exerciseRepository.save(Exercise.builder().exerciseName("Test bench")
                .muscleGroup(MuscleGroup.CHEST).trackingType(TrackingType.WEIGHT_REPS).build());
        pullUp = exerciseRepository.save(Exercise.builder().exerciseName("Test pull-up")
                .muscleGroup(MuscleGroup.BACK).trackingType(TrackingType.BODYWEIGHT).build());
    }

    private Workout saveWorkout(User owner, int dayOffset, boolean finished, Exercise exercise, double[]... sets) {
        Instant start = START.plus(Duration.ofDays(dayOffset));
        var workout = Workout.builder()
                .user(owner)
                .type(WorkoutType.STRENGTH)
                .bodyWeightKg(80.0)
                .startTime(start)
                .endTime(finished ? start.plus(Duration.ofHours(1)) : null)
                .build();
        var workoutExercise = WorkoutExercise.builder().workout(workout).exercise(exercise).orderIndex(0).build();
        for (int i = 0; i < sets.length; i++) {
            workoutExercise.getSets().add(ExerciseSet.builder()
                    .workoutExercise(workoutExercise)
                    .setNumber(i + 1)
                    .weight(sets[i][0])
                    .reps((int) sets[i][1])
                    .build());
        }
        workout.getExercises().add(workoutExercise);
        return workoutRepository.save(workout);
    }

    private static double[] set(double weight, int reps) {
        return new double[]{weight, reps};
    }

    @Test
    void sessionStats_forWeightExercise() {
        var first = saveWorkout(user, 0, true, bench, set(100, 5), set(110, 3));
        var second = saveWorkout(user, 7, true, bench, set(120, 1));
        saveWorkout(user, 8, false, bench, set(200, 1));
        saveWorkout(otherUser, 1, true, bench, set(300, 1));

        List<ExerciseSessionStat> stats = exerciseSetRepository.findSessionStats(user.getId(), bench.getId(), 0.0);

        assertThat(stats).extracting(ExerciseSessionStat::workoutId).containsExactly(first.getId(), second.getId());
        assertThat(stats.getFirst()).satisfies(s -> {
            assertThat(s.maxWeight()).isEqualTo(110.0);
            assertThat(s.bestOneRepMax()).isCloseTo(121.0, within(0.001));
            assertThat(s.bestSetVolume()).isCloseTo(500.0, within(0.001));
            assertThat(s.bestSessionVolume()).isCloseTo(830.0, within(0.001));
            assertThat(s.maxReps()).isEqualTo(5);
        });
        assertThat(stats.get(1).bestOneRepMax()).isCloseTo(120.0, within(0.001));
    }

    @Test
    void sessionStats_addBodyWeightWhenFactorIsOne() {
        saveWorkout(user, 0, true, pullUp, set(0, 10), set(0, 12));

        var stats = exerciseSetRepository.findSessionStats(user.getId(), pullUp.getId(), 1.0);

        assertThat(stats).singleElement().satisfies(s -> {
            assertThat(s.bestSetVolume()).isCloseTo(80.0 * 12, within(0.001));
            assertThat(s.bestSessionVolume()).isCloseTo(80.0 * 22, within(0.001));
            assertThat(s.maxReps()).isEqualTo(12);
        });
    }

    @Test
    void bestPerWorkout_groupsByExerciseAndWorkout() {
        var first = saveWorkout(user, 0, true, bench, set(100, 5), set(110, 3));
        var second = saveWorkout(user, 7, true, pullUp, set(0, 15));
        saveWorkout(user, 8, false, bench, set(200, 1));

        List<ExerciseWorkoutBest> rows = exerciseSetRepository.findBestPerWorkout(
                user.getId(), Set.of(bench.getId(), pullUp.getId()));

        assertThat(rows)
                .extracting(ExerciseWorkoutBest::exerciseId, ExerciseWorkoutBest::workoutId,
                        ExerciseWorkoutBest::maxWeight, ExerciseWorkoutBest::maxReps)
                .containsExactlyInAnyOrder(
                        tuple(bench.getId(), first.getId(), 110.0, 5),
                        tuple(pullUp.getId(), second.getId(), 0.0, 15));
    }
}
