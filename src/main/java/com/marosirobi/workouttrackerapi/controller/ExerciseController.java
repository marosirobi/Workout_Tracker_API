package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseStatsResponse;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseRepository exerciseRepository;
    private final StatService statService;

    @GetMapping
    public List<Exercise> getAllExercises(){
        return exerciseRepository.findAll();
    }

    @GetMapping("/{id}")
    public Exercise getExerciseById(@PathVariable Long id){
        return exerciseRepository.findById(id).orElse(null);
    }

    @GetMapping("/{id}/stats")
    public ExerciseStatsResponse getExerciseStats(@PathVariable Long id, Principal principal){
        return statService.getExerciseStats(id, principal.getName());
    }
}
