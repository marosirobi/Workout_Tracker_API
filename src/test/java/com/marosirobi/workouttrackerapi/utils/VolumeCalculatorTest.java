package com.marosirobi.workouttrackerapi.utils;

import com.marosirobi.workouttrackerapi.domain.entity.ExerciseSet;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VolumeCalculatorTest {

    @ParameterizedTest(name = "{0}: weight={1}, reps={2}, bodyWeight={3} -> {4}")
    @CsvSource(nullValues = "null", textBlock = """
            WEIGHT_REPS,         20,   10,   80,   200
            BODYWEIGHT,          0,    10,   80,   800
            WEIGHTED_BODYWEIGHT, 20,   10,   80,   1000
            DURATION,            20,   10,   80,   0
            WEIGHT_REPS,         null, 10,   80,   0
            WEIGHT_REPS,         20,   null, 80,   0
            BODYWEIGHT,          0,    10,   null, 0
            """)
    void setVolume(TrackingType type, Double weight, Integer reps, Double bodyWeight, double expected) {
        var set = ExerciseSet.builder().weight(weight).reps(reps).build();

        assertThat(VolumeCalculator.setVolume(type, set, bodyWeight)).isCloseTo(expected, within(0.0001));
    }
}
