package com.marosirobi.workouttrackerapi.config;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(1)
public class ExerciseSeeder implements CommandLineRunner {

    private final ExerciseRepository exerciseRepository;

    private static final Map<MuscleGroup, List<String>>  EXERCISES = Map.of(
            MuscleGroup.CHEST, List.of(
                    "Bench press", "Incline bench press", "Decline bench press",
                    "Dumbbell bench press", "Incline dumbbell press", "Machine chest press",
                    "Dumbbell fly", "Cable crossover", "Pec deck", "Chest dip", "Push-up"
            ),
            MuscleGroup.LEGS, List.of(
                    "Squat", "Front squat", "Leg press", "Romanian deadlift", "Lunge",
                    "Bulgarian split squat", "Leg extension", "Leg curl", "Hip thrust",
                    "Calf raise"
            ),
            MuscleGroup.BACK, List.of(
                    "Deadlift", "Pull-up", "Chin-up", "Lat pulldown", "Barbell row",
                    "Dumbbell row", "Seated cable row", "T-bar row", "Back extension"
            ),
            MuscleGroup.SHOULDERS, List.of(
                    "Overhead press", "Dumbbell shoulder press", "Arnold press",
                    "Lateral raise", "Front raise", "Rear delt fly", "Face pull",
                    "Upright row", "Shrug"
            ),
            MuscleGroup.BICEPS, List.of(
                    "Barbell curl", "Dumbbell curl", "Hammer curl", "Preacher curl",
                    "Cable curl", "Concentration curl", "Incline dumbbell curl"
            ),
            MuscleGroup.TRICEPS, List.of(
                    "Triceps pushdown", "Overhead triceps extension", "Skull crusher",
                    "Close-grip bench press", "Triceps dip", "Dumbbell kickback"
            ),
            MuscleGroup.ABS, List.of(
                    "Crunch", "Cable crunch", "Hanging leg raise", "Lying leg raise",
                    "Russian twist", "Ab wheel rollout"
            )
    );

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Set<String> existingNames = exerciseRepository.findAll().stream()
                .map(e -> e.getExerciseName().toLowerCase())
                .collect(Collectors.toSet());

        List<Exercise> toSave = new ArrayList<>();

        EXERCISES.forEach((group, names) -> {
            for(String name : names){
                if(existingNames.add(name.toLowerCase())){
                    Exercise exercise = new Exercise();
                    exercise.setExerciseName(name);
                    exercise.setMuscleGroup(group);
                    toSave.add(exercise);
                }
            }
        });

        if(!toSave.isEmpty()){
            exerciseRepository.saveAll(toSave);
            System.out.println("ExerciseSeeder: " + toSave.size() + " new exercise saved");
        }
    }
}
