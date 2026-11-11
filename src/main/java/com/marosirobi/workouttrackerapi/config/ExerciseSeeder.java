package com.marosirobi.workouttrackerapi.config;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup.*;
import static com.marosirobi.workouttrackerapi.domain.enums.TrackingType.*;

@Component
@RequiredArgsConstructor
@Order(1)
public class ExerciseSeeder implements CommandLineRunner {

    private record Seed(String name, MuscleGroup muscleGroup, TrackingType type) {}

    private static Seed weights(String name, MuscleGroup group) {
        return new Seed(name, group, WEIGHT_REPS);
    }

    private static final List<Seed> SEEDS = List.of(
            // Mell
            weights("Bench press", CHEST),
            weights("Incline bench press", CHEST),
            weights("Decline bench press", CHEST),
            weights("Dumbbell bench press", CHEST),
            weights("Incline dumbbell press", CHEST),
            weights("Machine chest press", CHEST),
            weights("Dumbbell fly", CHEST),
            weights("Cable crossover", CHEST),
            weights("Pec deck", CHEST),
            new Seed("Chest dip", CHEST, BODYWEIGHT),
            new Seed("Chest dip (weighted)", CHEST, WEIGHTED_BODYWEIGHT),
            new Seed("Push-up", CHEST, BODYWEIGHT),
            new Seed("Push-up (weighted)", CHEST, WEIGHTED_BODYWEIGHT),

            // Hat
            weights("Deadlift", BACK),
            new Seed("Pull-up", BACK, BODYWEIGHT),
            new Seed("Pull-up (weighted)", BACK, WEIGHTED_BODYWEIGHT),
            new Seed("Chin-up", BACK, BODYWEIGHT),
            new Seed("Chin-up (weighted)", BACK, WEIGHTED_BODYWEIGHT),
            weights("Lat pulldown", BACK),
            weights("Barbell row", BACK),
            weights("Dumbbell row", BACK),
            weights("Seated cable row", BACK),
            weights("T-bar row", BACK),
            weights("Back extension", BACK),
            new Seed("Dead hang", BACK, DURATION),

            // Lab
            weights("Squat", LEGS),
            weights("Front squat", LEGS),
            weights("Leg press", LEGS),
            weights("Romanian deadlift", LEGS),
            weights("Lunge", LEGS),
            weights("Bulgarian split squat", LEGS),
            weights("Leg extension", LEGS),
            weights("Leg curl", LEGS),
            weights("Hip thrust", LEGS),
            weights("Calf raise", LEGS),
            new Seed("Wall sit", LEGS, DURATION),

            // Vall
            weights("Overhead press", SHOULDERS),
            weights("Dumbbell shoulder press", SHOULDERS),
            weights("Arnold press", SHOULDERS),
            weights("Lateral raise", SHOULDERS),
            weights("Front raise", SHOULDERS),
            weights("Rear delt fly", SHOULDERS),
            weights("Face pull", SHOULDERS),
            weights("Upright row", SHOULDERS),
            weights("Shrug", SHOULDERS),

            // Bicepsz
            weights("Barbell curl", BICEPS),
            weights("Dumbbell curl", BICEPS),
            weights("Hammer curl", BICEPS),
            weights("Preacher curl", BICEPS),
            weights("Cable curl", BICEPS),
            weights("Concentration curl", BICEPS),
            weights("Incline dumbbell curl", BICEPS),

            // Tricepsz
            weights("Triceps pushdown", TRICEPS),
            weights("Overhead triceps extension", TRICEPS),
            weights("Skull crusher", TRICEPS),
            weights("Close-grip bench press", TRICEPS),
            new Seed("Triceps dip", TRICEPS, BODYWEIGHT),
            new Seed("Triceps dip (weighted)", TRICEPS, WEIGHTED_BODYWEIGHT),
            weights("Dumbbell kickback", TRICEPS),

            // Has
            new Seed("Crunch", ABS, BODYWEIGHT),
            weights("Cable crunch", ABS),
            new Seed("Hanging leg raise", ABS, BODYWEIGHT),
            new Seed("Lying leg raise", ABS, BODYWEIGHT),
            weights("Russian twist", ABS),
            weights("Ab wheel rollout", ABS),
            new Seed("Plank", ABS, DURATION),
            new Seed("Side plank", ABS, DURATION)
    );

    private final ExerciseRepository exerciseRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Exercise> existingByName = exerciseRepository.findAll().stream()
                .collect(Collectors.toMap(
                        e -> e.getExerciseName().toLowerCase(),
                        Function.identity(),
                        (first, second) -> first));

        List<Exercise> toSave = new ArrayList<>();
        int created = 0;
        int updated = 0;

        for (Seed seed : SEEDS) {
            Exercise existing = existingByName.get(seed.name().toLowerCase());

            if (existing == null) {
                Exercise exercise = new Exercise();
                exercise.setExerciseName(seed.name());
                exercise.setMuscleGroup(seed.muscleGroup());
                exercise.setTrackingType(seed.type());
                toSave.add(exercise);
                existingByName.put(seed.name().toLowerCase(), exercise);
                created++;
            } else if (existing.getTrackingType() != seed.type()) {
                existing.setTrackingType(seed.type());
                toSave.add(existing);
                updated++;
            }
        }

        if (!toSave.isEmpty()) {
            exerciseRepository.saveAll(toSave);
            System.out.println("ExerciseSeeder: " + created + " created, " + updated + " updated");
        }
    }
}
