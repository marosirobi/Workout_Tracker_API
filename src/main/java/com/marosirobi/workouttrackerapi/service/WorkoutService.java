package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioDataRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSetRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.workout.CreateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.UpdateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.WorkoutResponse;
import com.marosirobi.workouttrackerapi.dto.workout.WorkoutSummaryResponse;
import com.marosirobi.workouttrackerapi.exception.IllegalArgumentException;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.exception.UnauthorizedAccessException;
import com.marosirobi.workouttrackerapi.mapper.WorkoutMapper;
import com.marosirobi.workouttrackerapi.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final WorkoutMapper workoutMapper;

    @Transactional
    public WorkoutResponse createWorkout(CreateWorkoutRequest request, String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        WorkoutTemplate template = null;
        if(request.templateId() != null)
            template = workoutTemplateRepository.findById(request.templateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workout template not found " + request.templateId()));

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

        return workoutMapper.mapToResponse(savedWorkout);
    }

    @Transactional
    public void deleteWorkout(Long id, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        if(!workoutRepository.existsByIdAndUserId(id, user.getId())){
            throw new ResourceNotFoundException("Workout doesn't exist or you don't have permission: " + id);
        }
        workoutRepository.deleteById(id);
    }

    public Page<WorkoutSummaryResponse> getUserWorkouts(String email, int page, int size){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<Workout> workoutPage = workoutRepository.findByUserIdAndEndTimeIsNotNull(user.getId(), pageable);

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        Workout workout = workoutRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Workout not found or you don't have permission: " + id));

        return workoutMapper.mapToResponse(workout);
    }

    @Transactional
    public WorkoutResponse updateWorkout(Long id, UpdateWorkoutRequest request, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        Workout workout = workoutRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Workout not found " + id));

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
            if (workout.getEndTime() != null) {
                throw new IllegalArgumentException("this workout has already ended");
            }
            workout.getExercises().clear();

            for(WorkoutExerciseRequest exReq : request.exercises()){

                Exercise exercise = exerciseRepository.findById(exReq.exerciseId())
                        .orElseThrow(() -> new ResourceNotFoundException("Exercise not found " + exReq.exerciseId()));

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
        return workoutMapper.mapToResponse(updatedWorkout);
    }

    @Transactional
    public WorkoutResponse saveCardioData(Long workoutId, CardioDataRequest request, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout not found " + workoutId));

        if(!workout.getUser().getId().equals(user.getId())){
            throw new UnauthorizedAccessException("You dont have access to this workout");
        }

        if(!(workout.getType() == WorkoutType.CARDIO)){
            throw new IllegalArgumentException("Invalid workout type " + workout.getType());
        }

        if (workout.getEndTime() != null) {
            throw new IllegalArgumentException("This workout has already ended");
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
        
        if (request.workoutTitle() != null) {
            workout.setWorkoutTitle(request.workoutTitle());
        }
        if (request.workoutNote() != null) {
            workout.setWorkoutNote(request.workoutNote());
        }
        workout.setEndTime(request.endTime());

        workoutRepository.save(workout);

        return workoutMapper.mapToResponse(workout);
    }
}
