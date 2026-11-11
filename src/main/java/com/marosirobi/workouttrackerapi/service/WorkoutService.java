package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioDataRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSetRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.workout.*;
import com.marosirobi.workouttrackerapi.exception.IllegalArgumentException;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.exception.UnauthorizedAccessException;
import com.marosirobi.workouttrackerapi.mapper.WorkoutMapper;
import com.marosirobi.workouttrackerapi.repository.*;
import com.marosirobi.workouttrackerapi.utils.VolumeCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
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
    private final StatService statService;
    private final WorkoutMapper workoutMapper;

    private static final Duration MAX_ACTIVITY_RANGE = Duration.ofDays(400);

    @Transactional
    public WorkoutResponse createWorkout(CreateWorkoutRequest request, String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        if (user.getBodyWeightKg() == null) {
            throw new IllegalArgumentException("Body weight is required before starting a workout");
        }

        WorkoutTemplate template = null;
        if(request.templateId() != null) {
            if (request.type() != WorkoutType.STRENGTH)
                throw new IllegalArgumentException("Templates can only be used for strength workouts");

            template = workoutTemplateRepository.findByIdAndUserId(request.templateId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workout template not found " + request.templateId()));
        }
        Workout workout = Workout.builder()
                .user(user)
                .type(request.type())
                .startTime(request.startTime())
                .template(template)
                .bodyWeightKg(user.getBodyWeightKg())
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

    public List<ActivityWorkoutResponse> getWorkoutsInRange(Instant from, Instant to, String email) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        if (Duration.between(from, to).compareTo(MAX_ACTIVITY_RANGE) > 0) {
            throw new IllegalArgumentException("The requested range is too long");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        return workoutRepository
                .findByUserIdAndEndTimeIsNotNullAndStartTimeBetweenOrderByStartTimeAsc(user.getId(), from, to)
                .stream()
                .map(w -> new ActivityWorkoutResponse(w.getId(), w.getType(), w.getStartTime()))
                .toList();
    }

    @Transactional
    public Page<WorkoutSummaryResponse> getUserWorkouts(String email, int page, int size, WorkoutType type){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<Workout> workoutPage = type == null ?
                workoutRepository.findByUserIdAndEndTimeIsNotNull(user.getId(), pageable) :
                workoutRepository.findByUserIdAndTypeAndEndTimeIsNotNull(user.getId(), pageable, type);

        return workoutPage.map(workout -> {
            Double distance = null;
            String pace = null;
            Integer movingTime = null;

            if(workout.getCardioStats() != null){
                distance = workout.getCardioStats().getTotalDistance();
                pace = workout.getCardioStats().getAvgPace();
                movingTime = workout.getCardioStats().getMovingTimeSeconds();
            }

            Double totalVolume = null;
            Integer totalSets = null;
            Integer exerciseCount = null;

            if(workout.getType() == WorkoutType.STRENGTH && workout.getExercises() != null){
                exerciseCount = workout.getExercises().size();
                totalSets = workout.getExercises().stream()
                        .mapToInt(we -> we.getSets().size()).sum();
                Double bodyWeight = workout.getBodyWeightKg();
                totalVolume = workout.getExercises().stream()
                        .mapToDouble(we -> {
                            TrackingType trackingType = we.getExercise().effectiveTrackingType();
                            return we.getSets().stream()
                                    .mapToDouble(set -> VolumeCalculator.setVolume(trackingType, set, bodyWeight))
                                    .sum();
                        })
                        .sum();
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
                    .movingTimeSeconds(movingTime)
                    .avgPace(pace)
                    .totalDistance(distance)
                    .totalVolume(totalVolume)
                    .totalSets(totalSets)
                    .exerciseCount(exerciseCount)
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
            replaceExercises(workout,request.exercises());
        }

        if(request.endTime() != null){
            workout.setEndTime(request.endTime());
        }

        Workout updatedWorkout = workoutRepository.save(workout);
        return workoutMapper.mapToResponse(updatedWorkout);
    }

    @Transactional
    public WorkoutResponse editWorkout(Long id, EditWorkoutRequest request, String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        Workout workout = workoutRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Workout not found " + id));

        if (workout.getEndTime() == null) {
            throw new IllegalArgumentException("Only finished workouts can be edited");
        }
        if (request.workoutTitle() != null && !request.workoutTitle().isBlank()) {
            workout.setWorkoutTitle(request.workoutTitle().trim());
        }
        if (request.workoutNote() != null) {
            String note = request.workoutNote().trim();
            workout.setWorkoutNote(note.isEmpty() ? null : note);
        }

        if (request.exercises() != null) {
            if (workout.getType() != WorkoutType.STRENGTH) {
                throw new IllegalArgumentException("Only strength workouts have exercises");
            }
            if (request.exercises().isEmpty()) {
                throw new IllegalArgumentException("A strength workout needs at least one exercise");
            }
            replaceExercises(workout, request.exercises());
        }

        Workout saved = workoutRepository.save(workout);
        return workoutMapper.mapToResponse(saved);
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
                                .segment(dto.segment() != null ? dto.segment() : 0)
                                .timestamp(dto.timestamp())
                                .build())
                    .toList();

            trackPointRepository.saveAll(trackPointsToSave);
            statService.calculateAndSave(workout, trackPointsToSave);
        }
        
        if (request.workoutTitle() != null) {
            workout.setWorkoutTitle(request.workoutTitle());
        }
        if (request.workoutNote() != null) {
            workout.setWorkoutNote(request.workoutNote());
        }
        if(request.endTime() != null){
            workout.setEndTime(request.endTime());
        }

        workoutRepository.save(workout);

        return workoutMapper.mapToResponse(workout);
    }

    private void replaceExercises(Workout workout, List<WorkoutExerciseRequest> requests){
        workout.getExercises().clear();

        for(WorkoutExerciseRequest exReq : requests){

            Exercise exercise = exerciseRepository.findById(exReq.exerciseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Exercise not found " + exReq.exerciseId()));

            WorkoutExercise workoutExercise = WorkoutExercise.builder()
                    .workout(workout)
                    .exercise(exercise)
                    .orderIndex(exReq.orderIndex())
                    .build();

            if(exReq.sets() != null){
                TrackingType type = exercise.effectiveTrackingType();

                for(ExerciseSetRequest setReq : exReq.sets()){
                    ExerciseSet set = buildSet(workoutExercise, setReq, type);
                    workoutExercise.getSets().add(set);
                }
            }
            workout.getExercises().add(workoutExercise);
        }
    }

    private ExerciseSet buildSet(WorkoutExercise workoutExercise, ExerciseSetRequest setReq, TrackingType type) {
        ExerciseSet.ExerciseSetBuilder builder = ExerciseSet.builder()
                .workoutExercise(workoutExercise)
                .setNumber(setReq.setNumber());

        return switch (type) {
            case WEIGHT_REPS, WEIGHTED_BODYWEIGHT -> builder
                    .weight(setReq.weight())
                    .reps(setReq.reps())
                    .build();
            case BODYWEIGHT -> builder
                    .weight(0.0)
                    .reps(setReq.reps())
                    .build();
            case DURATION -> {
                if (setReq.durationSeconds() == null) {
                    throw new IllegalArgumentException("Duration is required for timed exercises");
                }
                yield builder
                        .weight(0.0)
                        .reps(0)
                        .durationSeconds(setReq.durationSeconds())
                        .build();
            }
        };
    }
}
