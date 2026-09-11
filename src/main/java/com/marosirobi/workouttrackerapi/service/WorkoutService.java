package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioDataRequest;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioStatsResponse;
import com.marosirobi.workouttrackerapi.dto.cardio.TrackPointResponse;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSetRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSetResponse;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseResponse;
import com.marosirobi.workouttrackerapi.dto.workout.CreateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.UpdateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.WorkoutResponse;
import com.marosirobi.workouttrackerapi.dto.workout.WorkoutSummaryResponse;
import com.marosirobi.workouttrackerapi.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final ExerciseRepository exerciseRepository;
    private final TrackPointRepository trackPointRepository;
    private final CardioStatsRepository cardioStatsRepository;

    @Transactional
    public WorkoutResponse createWorkout(CreateWorkoutRequest request, String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        WorkoutTemplate template = null;
        if(request.templateId() != null)
            template = workoutTemplateRepository.findById(request.templateId())
                    .orElseThrow(() -> new RuntimeException("Workout template not found " + request.templateId()));

        Workout workout = Workout.builder()
                .user(user)
                .type(request.type())
                .startTime(request.startTime())
                .template(template)
                .build();

            if(template != null && template.getExercises() != null){
                for(TemplateExercise tempEx : template.getExercises()){

                    WorkoutExercise workoutEx = WorkoutExercise.builder()
                            .orderIndex(tempEx.getOrderIndex())
                            .workout(workout)
                            .exercise(tempEx.getExercise())
                            .build();

                    for(int i = 1; i <= tempEx.getTargetSets(); i++){
                        ExerciseSet set = ExerciseSet.builder()
                                .workoutExercise(workoutEx)
                                .setNumber(i)
                                .reps(0)
                                .weight(0.0)
                                .build();

                        workoutEx.getSets().add(set);
                    }
                    workout.getExercises().add(workoutEx);
                }
            }

        Workout savedWorkout = workoutRepository.save(workout);

        return mapToResponse(savedWorkout);
    }

    @Transactional
    public void deleteWorkout(Long id, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        if(!workoutRepository.existsByIdAndUserId(id, user.getId())){
            throw new RuntimeException("Workout doesn't exist or you don't have permission: " + id);
        }
        workoutRepository.deleteById(id);
    }

    public Page<WorkoutSummaryResponse> getUserWorkouts(String email, int page, int size){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<Workout> workoutPage = workoutRepository.findByUserId(user.getId(), pageable);

        return workoutPage.map(workout -> {
            Double distance = null;
            String pace = null;

            if(workout.getCardioStats() != null){
                distance = workout.getCardioStats().getTotalDistance();
                pace = workout.getCardioStats().getAvgPace();
            }

            Long duration = 0L;

            if(workout.getStartTime() != null && workout.getEndTime() != null){
                duration = Duration.between(workout.getStartTime(), workout.getEndTime()).getSeconds();
            }
            return WorkoutSummaryResponse.builder()
                    .id(workout.getId())
                    .name(workout.getWorkoutTitle())
                    .type(workout.getType())
                    .startTime(workout.getStartTime())
                    .endTime(workout.getEndTime())
                    .durationSeconds(duration)
                    .avgPace(pace)
                    .totalDistance(distance)
                    .build();
        });
    }

    public WorkoutResponse getWorkoutById(Long id, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        Workout workout = workoutRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout not found or you don't have permission: " + id));

        return mapToResponse(workout);
    }

    @Transactional
    public WorkoutResponse updateWorkout(Long id, UpdateWorkoutRequest request, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        Workout workout = workoutRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout not found " + id));

        if(request.endTime() != null){
            workout.setEndTime(request.endTime());
        }
        if(request.workoutTitle() != null){
            workout.setWorkoutTitle(request.workoutTitle());
        }
        if(request.workoutNote() != null){
            workout.setWorkoutNote(request.workoutNote());
        }

        if(request.exercises() != null){
            workout.getExercises().clear();

            for(WorkoutExerciseRequest exReq : request.exercises()){

                Exercise exercise = exerciseRepository.findById(exReq.exerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found " + exReq.exerciseId()));

                WorkoutExercise workoutExercise = WorkoutExercise.builder()
                        .workout(workout)
                        .exercise(exercise)
                        .orderIndex(exReq.orderIndex())
                        .build();

                if(exReq.sets() != null){
                    for(ExerciseSetRequest setReq : exReq.sets()){
                        ExerciseSet set = ExerciseSet.builder()
                                .workoutExercise(workoutExercise)
                                .setNumber(setReq.setNumber())
                                .weight(setReq.weight())
                                .reps(setReq.reps())
                                .build();

                        workoutExercise.getSets().add(set);
                    }
                }
                workout.getExercises().add(workoutExercise);
            }
        }

        Workout updatedWorkout = workoutRepository.save(workout);
        return mapToResponse(updatedWorkout);
    }

    @Transactional
    public WorkoutResponse saveCardioData(Long workoutId, CardioDataRequest request, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout not found " + workoutId));

        if(!workout.getUser().getId().equals(user.getId())){
            throw new RuntimeException("You dont have access to this workout");
        }

        if(!(workout.getType() == WorkoutType.CARDIO)){
            throw new RuntimeException("Invalid workout type " + workout.getType());
        }

        CardioStats stats = CardioStats.builder()
                .workout(workout)
                .totalDistance(request.stats().totalDistance())
                .avgPace(request.stats().avgPace())
                .movingTimeSeconds(request.stats().movingTimeSeconds())
                .elevationGain(request.stats().elevationGain())
                .build();

        cardioStatsRepository.save(stats);

        if(request.trackPoints() != null && !request.trackPoints().isEmpty()){
            List<TrackPoint> trackPointsToSave = request.trackPoints().stream()
                    .map(dto -> TrackPoint.builder()
                                .workout(workout)
                                .latitude(dto.latitude())
                                .longitude(dto.longitude())
                                .elevation(dto.elevation())
                                .speed(dto.speed())
                                .timestamp(dto.timestamp())
                                .build())
                    .toList();

            trackPointRepository.saveAll(trackPointsToSave);
        }

        workout.setEndTime(LocalDateTime.now());

        workoutRepository.save(workout);

        return mapToResponse(workout);
    }

    private WorkoutResponse mapToResponse(Workout workout){
        WorkoutResponse.WorkoutResponseBuilder responseBuilder = WorkoutResponse.builder()
                .id(workout.getId())
                .workoutTitle(workout.getWorkoutTitle())
                .workoutNote(workout.getWorkoutNote())
                .startTime(workout.getStartTime())
                .endTime(workout.getEndTime())
                .type(workout.getType());

        if(workout.getType() == WorkoutType.STRENGTH){
            if(workout.getTemplate() != null){
                responseBuilder.templateId(workout.getTemplate().getId());
            }
            if(workout.getExercises() != null){
                List<WorkoutExerciseResponse> exerciseResponses = workout.getExercises()
                        .stream()
                        .map(we -> {
                            WorkoutExerciseResponse.WorkoutExerciseResponseBuilder weResponseBuilder = WorkoutExerciseResponse.builder()
                            .id(we.getId())
                            .orderIndex(we.getOrderIndex());

                            if(we.getExercise() != null){
                                weResponseBuilder
                                        .exerciseId(we.getExercise().getId())
                                        .exerciseName(we.getExercise().getExerciseName());
                            }

                            if(we.getSets() != null){
                                List<ExerciseSetResponse> setResponses = we.getSets()
                                        .stream()
                                        .map(set -> ExerciseSetResponse.builder()
                                                .id(set.getId())
                                                .setNumber(set.getSetNumber())
                                                .reps(set.getReps())
                                                .weight(set.getWeight()).build()
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
                                .elevation(tp.getElevation())
                                .build()
                        ).toList();
                responseBuilder.trackPoints(trackPoints);
            }
        }

        return responseBuilder.build();
    }
}
