package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.ExerciseSet;
import com.marosirobi.workouttrackerapi.dto.stat.ExerciseMaxWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, Long> {

    @Query("SELECT MAX(s.weight) FROM ExerciseSet s " +
            "JOIN s.workoutExercise we " +
            "JOIN we.workout w " +
            "WHERE w.user.id = :userId AND we.exercise.id = :exerciseId")
    Double findPersonalRecord(@Param("userId") Long userId, @Param("exerciseId") Long exerciseId);

    @Query("SELECT new com.marosirobi.workouttrackerapi.dto.stat.ExerciseMaxWeight(" +
            "we.exercise.id, w.id, w.startTime, MAX(s.weight)) " +
            "FROM ExerciseSet s " +
            "JOIN s.workoutExercise we " +
            "JOIN we.workout w " +
            "WHERE w.user.id = :userId " +
            "AND we.exercise.id IN :exerciseIds " +
            "AND w.endTime IS NOT NULL " +
            "GROUP BY we.exercise.id, w.id, w.startTime")
    List<ExerciseMaxWeight> findMaxWeightPerWorkout(@Param("userId") Long userId, @Param("exerciseIds")Collection<Long> exerciseIds);
}
