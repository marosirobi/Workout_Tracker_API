package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.dto.cardio.CardioDataRequest;
import com.marosirobi.workouttrackerapi.dto.workout.*;
import com.marosirobi.workouttrackerapi.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
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

    @PostMapping("/{id}/cardio-data")
    public WorkoutResponse saveCardioData(@PathVariable Long id,
                                          @Valid @RequestBody CardioDataRequest request,
                                          Principal principal){
        return workoutService.saveCardioData(id, request, principal.getName());
    }

    @PutMapping("/{id}")
    public WorkoutResponse updateWorkout(@PathVariable Long id,
                                         @Valid @RequestBody UpdateWorkoutRequest request,
                                         Principal principal){
        return workoutService.updateWorkout(id, request, principal.getName());
    }

    @PutMapping("/{id}/edit")
    public WorkoutResponse editWorkout(@PathVariable Long id,
                                       @Valid @RequestBody EditWorkoutRequest request,
                                       Principal principal){
        return workoutService.editWorkout(id, request, principal.getName());
    }

    @GetMapping
    public Page<WorkoutSummaryResponse> getUserWorkouts(Principal principal,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false)WorkoutType type,
                                                        @RequestParam(defaultValue = "0") int page){
        return workoutService.getUserWorkouts(principal.getName(), page, size, type);
    }

    @GetMapping("/range")
    public List<ActivityWorkoutResponse> getWorkoutsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Principal principal) {
        return workoutService.getWorkoutsInRange(from, to, principal.getName());
    }

    @GetMapping("/{id}")
    public WorkoutResponse getWorkoutById(@PathVariable Long id, Principal principal){
        return workoutService.getWorkoutById(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkout(@PathVariable Long id,
                              Principal principal){
        workoutService.deleteWorkout(id, principal.getName());
    }
}
