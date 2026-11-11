package com.marosirobi.workouttrackerapi.utils;

import com.marosirobi.workouttrackerapi.domain.entity.ExerciseSet;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;

public final class VolumeCalculator {

    private VolumeCalculator() {}

    public static double setVolume(TrackingType type, ExerciseSet set, Double bodyWeightKg) {
        double bodyWeight = bodyWeightKg != null ? bodyWeightKg : 0;
        double weight = set.getWeight() != null ? set.getWeight() : 0;
        int reps = set.getReps() != null ? set.getReps() : 0;

        return switch (type) {
            case WEIGHT_REPS -> weight * reps;
            case BODYWEIGHT -> bodyWeight * reps;
            case WEIGHTED_BODYWEIGHT -> (bodyWeight + weight) * reps;
            case DURATION -> 0;
        };
    }
}
