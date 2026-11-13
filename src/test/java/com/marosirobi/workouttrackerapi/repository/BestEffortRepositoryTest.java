package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.BestEffort;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.domain.entity.Workout;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BestEffortRepositoryTest {

    @Autowired private BestEffortRepository bestEffortRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkoutRepository workoutRepository;

    private Workout cardioWorkout(User owner, double... efforts1k) {
        var workout = Workout.builder().user(owner).type(WorkoutType.CARDIO)
                .startTime(Instant.now()).endTime(Instant.now()).build();
        for (double duration : efforts1k) {
            workout.getBestEfforts().add(BestEffort.builder().workout(workout)
                    .distanceMeters(1000).durationSeconds(duration).startOffsetMeters(0.0).build());
        }
        return workoutRepository.save(workout);
    }

    @Test
    void countsOnlyFasterEffortsOfSameUserAndDistance() {
        var user = userRepository.save(User.builder().username("user").email("user@test.hu").password("x").build());
        var other = userRepository.save(User.builder().username("other").email("other@test.hu").password("x").build());
        cardioWorkout(user, 240.0, 250.0, 300.0);
        cardioWorkout(other, 200.0);

        long faster = bestEffortRepository
                .countByWorkoutUserIdAndDistanceMetersAndDurationSecondsLessThan(user.getId(), 1000, 260.0);

        assertThat(faster).isEqualTo(2);
    }
}
