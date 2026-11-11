package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateRequest;
import com.marosirobi.workouttrackerapi.dto.template.WorkoutTemplateResponse;
import com.marosirobi.workouttrackerapi.service.WorkoutTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class WorkoutTemplateController {

    private final WorkoutTemplateService workoutTemplateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutTemplateResponse createTemplate(@RequestBody @Valid WorkoutTemplateRequest request,
                                                  Principal principal){
        return workoutTemplateService.createTemplate(request, principal.getName());
    }

    @PutMapping("/{id}")
    public WorkoutTemplateResponse updateTemplate(@PathVariable Long id,
                                                  @Valid @RequestBody WorkoutTemplateRequest request,
                                                  Principal principal){
        return workoutTemplateService.updateTemplate(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Long id,
                               Principal principal){
        workoutTemplateService.deleteTemplate(id, principal.getName());
    }

    @GetMapping
    public List<WorkoutTemplateResponse> getTemplatesByUserId(Principal principal){
        return workoutTemplateService.getMyTemplates(principal.getName());
    }
}
