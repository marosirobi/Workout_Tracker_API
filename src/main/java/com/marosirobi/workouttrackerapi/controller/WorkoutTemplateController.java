package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.dto.template.CreateWorkoutTemplateRequest;
import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateResponse;
import com.marosirobi.workouttrackerapi.service.WorkoutTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class WorkoutTemplateController {

    private final WorkoutTemplateService workoutTemplateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutTemplateResponse createTemplate(@RequestBody CreateWorkoutTemplateRequest request){
        return workoutTemplateService.createTemplate(request);
    }

    @GetMapping("/users/{userId}")
    public List<WorkoutTemplateResponse> getTemplatesByUserId(@PathVariable Long userId){
        return workoutTemplateService.getTemplatesByUserId(userId);
    }
}
