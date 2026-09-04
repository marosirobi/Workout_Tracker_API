package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import com.marosirobi.workouttrackerapi.domain.entity.User;
import com.marosirobi.workouttrackerapi.dto.stat.PersonalRecordResponse;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.ExerciseSetRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatService {

    private final ExerciseSetRepository exerciseSetRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;

    public PersonalRecordResponse getPersonalRecord(Long exerciseId, String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found " + email));

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found " + exerciseId));

        Double maxWeight = exerciseSetRepository.findPersonalRecord(user.getId(), exercise.getId());

        //map to personal record response
        //later we will need other stats so i wont make a generic mapToResponse func
        return PersonalRecordResponse.builder()
                .exerciseId(exercise.getId())
                .maxWeight(maxWeight)
                .exerciseName(exercise.getExerciseName())
                .build();
    }
}
