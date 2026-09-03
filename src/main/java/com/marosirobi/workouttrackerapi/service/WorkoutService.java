package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSetRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSetResponse;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.exercise.WorkoutExerciseResponse;
import com.marosirobi.workouttrackerapi.dto.template.TemplateExerciseRequest;
import com.marosirobi.workouttrackerapi.dto.workout.CreateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.UpdateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.WorkoutResponse;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import com.marosirobi.workouttrackerapi.repository.WorkoutRepository;
import com.marosirobi.workouttrackerapi.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutResponse createWorkout(CreateWorkoutRequest request, String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        Workout workout = new Workout();
        workout.setUser(user);
        workout.setType(request.getType());
        workout.setStartTime(request.getStartTime());

        if(request.getTemplateId() != null){
            WorkoutTemplate template = workoutTemplateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Workout template not found " + request.getTemplateId()));

            workout.setTemplate(template);

            if(template.getExercises() != null){
                for(TemplateExercise tempEx : template.getExercises()){

                    WorkoutExercise workoutEx = new WorkoutExercise();
                    workoutEx.setOrderIndex(tempEx.getOrderIndex());
                    workoutEx.setWorkout(workout);
                    workoutEx.setExercise(tempEx.getExercise());

                    for(int i = 1; i <= tempEx.getTargetSets(); i++){
                        ExerciseSet set = new ExerciseSet();
                        set.setSetNumber(i);
                        set.setWorkoutExercise(workoutEx);
                        set.setReps(0);
                        set.setWeight(0.0);
                        workoutEx.getSets().add(set);
                    }
                    workout.getExercises().add(workoutEx);
                }
            }
        }

        Workout savedWorkout = workoutRepository.save(workout);

        return mapToResponse(savedWorkout);
    }

    public List<WorkoutResponse> getWorkoutsByUserId(Long userId){

        if(!userRepository.existsById(userId)){
            throw new RuntimeException("User not found " + userId);
        }

        return workoutRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteWorkout(Long id){
        if(!workoutRepository.existsById(id)){
            throw new RuntimeException("workout doesn't exist " + id);
        }
        workoutRepository.deleteById(id);
    }

    public WorkoutResponse updateWorkout(Long id, UpdateWorkoutRequest request){
        Workout workout = workoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout not found " + id));

        if(request.getEndTime() != null){
            workout.setEndTime(request.getEndTime());
        }
        if(request.getWorkoutTitle() != null){
            workout.setWorkoutTitle(request.getWorkoutTitle());
        }
        if(request.getWorkoutNote() != null){
            workout.setWorkoutNote(request.getWorkoutNote());
        }

        if(request.getExercises() != null){
            workout.getExercises().clear();

            for(WorkoutExerciseRequest exReq : request.getExercises()){

                Exercise exercise = exerciseRepository.findById(exReq.getExerciseId())
                        .orElseThrow(() -> new RuntimeException("Exercise not found " + exReq.getExerciseId()));

                WorkoutExercise workoutExercise = new WorkoutExercise();
                workoutExercise.setWorkout(workout);
                workoutExercise.setExercise(exercise);
                workoutExercise.setOrderIndex(exReq.getOrderIndex());

                if(exReq.getSets() != null){
                    for(ExerciseSetRequest setReq : exReq.getSets()){
                        ExerciseSet set = new ExerciseSet();
                        set.setWorkoutExercise(workoutExercise);
                        set.setSetNumber(setReq.getSetNumber());
                        set.setWeight(setReq.getWeight());
                        set.setReps(setReq.getReps());

                        workoutExercise.getSets().add(set);
                    }
                }
                workout.getExercises().add(workoutExercise);
            }
        }

        Workout updatedWorkout = workoutRepository.save(workout);
        return mapToResponse(updatedWorkout);
    }

    private WorkoutResponse mapToResponse(Workout workout){
        WorkoutResponse response = new WorkoutResponse();
        response.setId(workout.getId());
        response.setWorkoutTitle(workout.getWorkoutTitle());
        response.setWorkoutNote(workout.getWorkoutNote());
        response.setType(workout.getType());
        response.setStartTime(workout.getStartTime());
        response.setEndTime(workout.getEndTime());
        if(workout.getTemplate() != null){
            response.setTemplateId(workout.getTemplate().getId());
        }
        if(workout.getExercises() != null){
            List<WorkoutExerciseResponse> exerciseResponses = workout.getExercises()
                    .stream()
                    .map(we -> {
                        WorkoutExerciseResponse weResp = new WorkoutExerciseResponse();
                        weResp.setId(we.getId());
                        weResp.setOrderIndex(we.getOrderIndex());

                        if(we.getExercise() != null){
                            weResp.setExerciseId(we.getExercise().getId());
                            weResp.setExerciseName(we.getExercise().getExerciseName());
                        }

                        if(we.getSets() != null){
                            List<ExerciseSetResponse> setResponses = we.getSets()
                                    .stream()
                                    .map(set -> {
                                        ExerciseSetResponse setResp = new ExerciseSetResponse();
                                        setResp.setId(set.getId());
                                        setResp.setSetNumber(set.getSetNumber());
                                        setResp.setReps(set.getReps());
                                        setResp.setWeight(set.getWeight());
                                        return setResp;
                                    })
                                    .collect(Collectors.toList());

                            weResp.setSets(setResponses);
                        }
                        return weResp;
                    })
                    .collect(Collectors.toList());

            response.setExercises(exerciseResponses);
        }
        return response;
    }

}
