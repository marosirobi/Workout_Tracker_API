package com.marosirobi.workouttrackerapi.dto.workout;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioStatsResponse;
import com.marosirobi.workouttrackerapi.dto.cardio.TrackPointResponse;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@JsonPropertyOrder({
        "id",
        "templateId",
        "startTime",
        "endTime",
        "type",
        "workoutTitle",
        "workoutNote",
        "exercises"
})

@Builder
public record WorkoutResponse(Long id,
                              Long templateId,
                              String workoutTitle,
                              String workoutNote,
                              WorkoutType type,
                              LocalDateTime startTime,
                              LocalDateTime endTime,
                              //STRENGTH
                              List<WorkoutExerciseResponse> exercises,
                              //CARDIO
                              CardioStatsResponse cardioStats,
                              List<TrackPointResponse> trackPoints) { }
