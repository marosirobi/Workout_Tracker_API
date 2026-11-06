package com.marosirobi.workouttrackerapi.dto.exercise;

import lombok.Builder;
import java.time.Instant;

@Builder
public record ExerciseSessionStat(Long workoutId,
                                  Instant startTime,
                                  Double maxWeight,
                                  Double bestOneRepMax,
                                  Double bestSetVolume,
                                  Double bestSessionVolume) {
}
