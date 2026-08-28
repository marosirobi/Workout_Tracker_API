package com.marosirobi.workouttrackerapi.config;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import com.marosirobi.workouttrackerapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final ExerciseSetRepository exerciseSetRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {



        if(userRepository.count() == 0){
            // add test user
            User testUser = new User();
            testUser.setUsername("Bajnok");
            testUser.setEmail("bajnok@gmail.com");
            testUser.setPassword(passwordEncoder.encode("jelszo"));

            userRepository.save(testUser);
            System.out.println("TestUser data is saved!");

            //add test exercises
            Exercise benchPress = new Exercise();
            benchPress.setExerciseName("Bench press");
            benchPress.setMuscleGroup(MuscleGroup.CHEST);
            exerciseRepository.save(benchPress);

            Exercise squat = new Exercise();
            squat.setExerciseName("Squat");
            squat.setMuscleGroup(MuscleGroup.LEGS);
            exerciseRepository.save(squat);

            System.out.println("Test exercises are saved!");

            //add test workout
            Workout workout = new Workout();
            workout.setType(WorkoutType.STRENGTH);
            workout.setUser(testUser);
            workout.setStartTime(LocalDateTime.now());
            workoutRepository.save(workout);

            System.out.println("Test workout is saved!");

            //add test workoutExercise and ExerciseSet data
            WorkoutExercise workoutExercise = new WorkoutExercise();
            workoutExercise.setWorkout(workout);
            workoutExercise.setExercise(benchPress);
            workoutExercise.setOrderIndex(1);

            List<ExerciseSet> sets = new ArrayList<>();
            ExerciseSet exerciseSet1 = new ExerciseSet();
            exerciseSet1.setSetNumber(1);
            exerciseSet1.setReps(4);
            exerciseSet1.setWeight(90.00);
            exerciseSet1.setWorkoutExercise(workoutExercise);

            ExerciseSet exerciseSet2 = new ExerciseSet();
            exerciseSet2.setSetNumber(2);
            exerciseSet2.setReps(3);
            exerciseSet2.setWeight(90.00);
            exerciseSet2.setWorkoutExercise(workoutExercise);

            sets.add(exerciseSet1);
            sets.add(exerciseSet2);

            workoutExercise.setSets(sets);

            workoutExerciseRepository.save(workoutExercise);
            exerciseSetRepository.save(exerciseSet1);
            exerciseSetRepository.save(exerciseSet2);

            System.out.println("Test workoutExercise and exerciseSets are saved!");
        }

    }
}
