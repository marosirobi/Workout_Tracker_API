package com.marosirobi.workouttrackerapi.dto.exercise;

import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import lombok.Builder;

import java.util.List;

@Builder
public record ExerciseStatsResponse(Long exerciseId,
                                    String exerciseName,
                                    MuscleGroup muscleGroup,
                                    ExerciseRecord heaviestWeight,
                                    ExerciseRecord bestOneRepMax,
                                    ExerciseRecord bestSetVolume,
                                    ExerciseRecord bestSessionVolume,
                                    List<ExerciseSessionStat> history) {
}
