package com.marosirobi.workouttrackerapi.config;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;

    @Override
    public void run(String... args) throws Exception {

        if(userRepository.count() == 0){
            // add test user
            User testUser = new User();
            testUser.setUsername("Bajnok");
            testUser.setEmail("bajnok@gmail.com");
            testUser.setPassword("jelszo");

            userRepository.save(testUser);
            System.out.println("TestUser is saved!");

            //add test exercise data
            Exercise benchPress = new Exercise();
            benchPress.setExerciseName("Bench press");
            benchPress.setMuscleGroup(MuscleGroup.CHEST);
            exerciseRepository.save(benchPress);

            Exercise squat = new Exercise();
            squat.setExerciseName("Squat");
            squat.setMuscleGroup(MuscleGroup.LEGS);
            exerciseRepository.save(squat);

            System.out.println("Test exercises data are saved!");
        }

    }
}
