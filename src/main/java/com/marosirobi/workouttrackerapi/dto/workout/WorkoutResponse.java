package com.marosirobi.workouttrackerapi.dto.workout;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioStatsDto;
import com.marosirobi.workouttrackerapi.dto.cardio.TrackPointDto;
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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutResponse {
    private Long id;
    private Long templateId;
    private String workoutTitle;
    private String workoutNote;
    private WorkoutType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    //STRENGTH
    private List<WorkoutExerciseResponse> exercises;

    //CARDIO
    private CardioStatsDto cardioStats;
    private List<TrackPointDto> trackPoints;
}
