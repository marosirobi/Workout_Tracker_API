package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseRepository exerciseRepository;

    @GetMapping
    public List<Exercise> getAllExercises(){
        return exerciseRepository.findAll();
    }
}
