package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    Optional<Exercise> findByExerciseName(String exerciseName);
}
