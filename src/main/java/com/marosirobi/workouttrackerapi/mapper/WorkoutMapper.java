package com.marosirobi.workouttrackerapi.mapper;

import com.marosirobi.workouttrackerapi.domain.entity.BestEffort;
import com.marosirobi.workouttrackerapi.domain.entity.Workout;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioStatsResponse;
import com.marosirobi.workouttrackerapi.dto.cardio.TrackPointResponse;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSetResponse;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseResponse;
import com.marosirobi.workouttrackerapi.dto.stat.BestEffortResponse;
import com.marosirobi.workouttrackerapi.dto.workout.WorkoutResponse;
import com.marosirobi.workouttrackerapi.repository.BestEffortRepository;
import com.marosirobi.workouttrackerapi.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkoutMapper {

    private final BestEffortRepository bestEffortRepository;
    private final StatService statService;

    public WorkoutResponse mapToResponse(Workout workout){
        WorkoutResponse.WorkoutResponseBuilder responseBuilder = WorkoutResponse.builder()
                .id(workout.getId())
                .workoutTitle(workout.getWorkoutTitle())
                .workoutNote(workout.getWorkoutNote())
                .startTime(workout.getStartTime())
                .endTime(workout.getEndTime())
                .type(workout.getType())
                .bodyWeightKg(workout.getBodyWeightKg());

        if(workout.getType() == WorkoutType.STRENGTH){
            if(workout.getTemplate() != null){
                responseBuilder.templateId(workout.getTemplate().getId());
            }
            if(workout.getExercises() != null){
                Map<Long, Integer> weightRanks = statService.computeWeightRanks(workout);

                List<WorkoutExerciseResponse> exerciseResponses = workout.getExercises()
                        .stream()
                        .map(we -> {
                            WorkoutExerciseResponse.WorkoutExerciseResponseBuilder weResponseBuilder = WorkoutExerciseResponse.builder()
                                    .id(we.getId())
                                    .orderIndex(we.getOrderIndex());

                            if(we.getExercise() != null){
                                weResponseBuilder
                                        .exerciseId(we.getExercise().getId())
                                        .muscleGroup(we.getExercise().getMuscleGroup())
                                        .trackingType(we.getExercise().effectiveTrackingType())
                                        .weightRank(weightRanks.get(we.getExercise().getId()))
                                        .exerciseName(we.getExercise().getExerciseName());
                            }

                            if(we.getSets() != null){
                                List<ExerciseSetResponse> setResponses = we.getSets()
                                        .stream()
                                        .map(set -> ExerciseSetResponse.builder()
                                                .id(set.getId())
                                                .setNumber(set.getSetNumber())
                                                .reps(set.getReps())
                                                .weight(set.getWeight())
                                                .durationSeconds(set.getDurationSeconds())
                                                .build()
                                        ).toList();
                                weResponseBuilder.sets(setResponses);
                            }
                            return weResponseBuilder.build();
                        }).toList();
                responseBuilder.exercises(exerciseResponses);
            }
        }else if(workout.getType() == WorkoutType.CARDIO){
            if(workout.getCardioStats() != null){
                responseBuilder.cardioStats(CardioStatsResponse.builder()
                        .totalDistance(workout.getCardioStats().getTotalDistance())
                        .elevationGain(workout.getCardioStats().getElevationGain())
                        .movingTimeSeconds(workout.getCardioStats().getMovingTimeSeconds())
                        .avgPace(workout.getCardioStats().getAvgPace())
                        .build()
                );
            }
            if(workout.getTrackPoints() != null){
                List<TrackPointResponse> trackPoints = workout.getTrackPoints()
                        .stream()
                        .map(tp -> TrackPointResponse.builder()
                                .timestamp(tp.getTimestamp())
                                .latitude(tp.getLatitude())
                                .longitude(tp.getLongitude())
                                .speed(tp.getSpeed())
                                .segment(tp.getSegment() != null ? tp.getSegment() : 0)
                                .elevation(tp.getElevation())
                                .build()
                        ).toList();
                responseBuilder.trackPoints(trackPoints);
            }
            if(workout.getBestEfforts() != null && !workout.getBestEfforts().isEmpty()){
                Long userId = workout.getUser().getId();

                List<BestEffortResponse> efforts = workout.getBestEfforts()
                        .stream()
                        .sorted(Comparator.comparing(BestEffort::getDistanceMeters))
                        .map(effort -> BestEffortResponse.builder()
                                .distanceMeters(effort.getDistanceMeters())
                                .durationSeconds(effort.getDurationSeconds())
                                .startOffsetMeters(effort.getStartOffsetMeters())
                                .rank((int) bestEffortRepository
                                        .countByWorkoutUserIdAndDistanceMetersAndDurationSecondsLessThan(
                                                userId, effort.getDistanceMeters(), effort.getDurationSeconds()) + 1)
                                .build()
                        ).toList();
            responseBuilder.bestEfforts(efforts);
            }
        }

        return responseBuilder.build();
    }
}
