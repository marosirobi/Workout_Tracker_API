package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.dto.workout.CreateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.UpdateWorkoutRequest;
import com.marosirobi.workouttrackerapi.dto.workout.WorkoutResponse;
import com.marosirobi.workouttrackerapi.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse createWorkout(@Valid @RequestBody CreateWorkoutRequest request, Principal principal){
        return workoutService.createWorkout(request, principal.getName());
    }

    @PutMapping("/{id}")
    public WorkoutResponse updateWorkout(@PathVariable Long id, @Valid @RequestBody UpdateWorkoutRequest request){
        return workoutService.updateWorkout(id, request);
    }

    @GetMapping("/user/{userId}")
    public List<WorkoutResponse> getWorkoutsByUserId(@PathVariable Long userId){
        return workoutService.getWorkoutsByUserId(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkout(@PathVariable Long id){
        workoutService.deleteWorkout(id);
    }
}
