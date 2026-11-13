package com.marosirobi.workouttrackerapi.mapper;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.TrackPointResponse;
import com.marosirobi.workouttrackerapi.dto.stat.BestEffortResponse;
import com.marosirobi.workouttrackerapi.repository.BestEffortRepository;
import com.marosirobi.workouttrackerapi.service.StatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutMapperTest {

    private static final Instant START = Instant.parse("2026-11-01T08:00:00Z");

    @Mock private BestEffortRepository bestEffortRepository;
    @Mock private StatService statService;

    @InjectMocks
    private WorkoutMapper workoutMapper;

    private final User user = User.builder().id(1L).build();

    @Test
    void strengthWorkout_mapsExercisesSetsAndRanks() {
        var bench = Exercise.builder().id(1L).exerciseName("Bench press")
                .muscleGroup(MuscleGroup.CHEST).trackingType(TrackingType.WEIGHT_REPS).build();
        var workout = Workout.builder().id(10L).user(user).type(WorkoutType.STRENGTH).startTime(START)
                .template(WorkoutTemplate.builder().id(5L).build()).bodyWeightKg(80.0).build();
        var workoutExercise = WorkoutExercise.builder().id(20L).exercise(bench).orderIndex(0).build();
        workoutExercise.getSets().add(ExerciseSet.builder().id(30L).setNumber(1).weight(100.0).reps(5).build());
        workout.getExercises().add(workoutExercise);
        given(statService.computeWeightRanks(workout)).willReturn(Map.of(1L, 2));

        var response = workoutMapper.mapToResponse(workout);

        assertThat(response.templateId()).isEqualTo(5L);
        assertThat(response.bodyWeightKg()).isEqualTo(80.0);
        assertThat(response.exercises()).singleElement().satisfies(we -> {
            assertThat(we.exerciseName()).isEqualTo("Bench press");
            assertThat(we.weightRank()).isEqualTo(2);
            assertThat(we.trackingType()).isEqualTo(TrackingType.WEIGHT_REPS);
            assertThat(we.sets()).singleElement()
                    .returns(100.0, s -> s.weight())
                    .returns(5, s -> s.reps());
        });
        assertThat(response.cardioStats()).isNull();
        then(bestEffortRepository).shouldHaveNoInteractions();
    }

    @Test
    void cardioWorkout_mapsStatsPointsAndRankedEfforts() {
        var workout = Workout.builder().id(10L).user(user).type(WorkoutType.CARDIO).startTime(START)
                .cardioStats(CardioStats.builder().totalDistance(5.0).avgPace("6:00").elevationGain(20).movingTimeSeconds(1800).build())
                .build();
        workout.getTrackPoints().add(TrackPoint.builder().latitude(47.0).longitude(19.0).elevation(100.0)
                .speed(3.0).segment(null).timestamp(START).build());
        workout.getBestEfforts().add(BestEffort.builder().distanceMeters(1000).durationSeconds(300.0).startOffsetMeters(0.0).build());
        workout.getBestEfforts().add(BestEffort.builder().distanceMeters(400).durationSeconds(110.0).startOffsetMeters(100.0).build());
        given(bestEffortRepository.countByWorkoutUserIdAndDistanceMetersAndDurationSecondsLessThan(1L, 400, 110.0)).willReturn(0L);
        given(bestEffortRepository.countByWorkoutUserIdAndDistanceMetersAndDurationSecondsLessThan(1L, 1000, 300.0)).willReturn(2L);

        var response = workoutMapper.mapToResponse(workout);

        assertThat(response.cardioStats().totalDistance()).isEqualTo(5.0);
        assertThat(response.trackPoints()).singleElement().returns(0, TrackPointResponse::segment);
        assertThat(response.bestEfforts())
                .extracting(BestEffortResponse::distanceMeters, BestEffortResponse::rank)
                .containsExactly(tuple(400, 1), tuple(1000, 3));
        assertThat(response.exercises()).isNull();
        then(statService).shouldHaveNoInteractions();
    }

    @Test
    void cardioWorkout_withoutData_hasNoStatsOrEfforts() {
        var workout = Workout.builder().id(10L).user(user).type(WorkoutType.CARDIO).startTime(START).build();

        var response = workoutMapper.mapToResponse(workout);

        assertThat(response.cardioStats()).isNull();
        assertThat(response.trackPoints()).isEmpty();
        assertThat(response.bestEfforts()).isNull();
    }
}
