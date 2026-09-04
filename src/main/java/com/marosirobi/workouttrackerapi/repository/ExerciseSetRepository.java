package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.ExerciseSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, Long> {

    @Query("SELECT MAX(s.weight) FROM ExerciseSet s " +
            "JOIN s.workoutExercise we " +
            "JOIN we.workout w " +
            "WHERE w.user.id = :userId AND we.exercise.id = :exerciseId")
    Double findPersonalRecord(@Param("userId") Long userId, @Param("exerciseId") Long exerciseId);
}
