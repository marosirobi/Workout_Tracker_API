package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.ExerciseSet;
import com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSessionStat;
import com.marosirobi.workouttrackerapi.dto.stat.ExerciseWorkoutBest;
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

    @Query("SELECT new com.marosirobi.workouttrackerapi.dto.stat.ExerciseWorkoutBest(" +
            "we.exercise.id, w.id, w.startTime, " +
            "CAST(MAX(s.weight) AS DOUBLE), " +
            "CAST(MAX(s.reps) AS INTEGER), " +
            "CAST(MAX(s.durationSeconds) AS INTEGER)) " +
            "FROM ExerciseSet s " +
            "JOIN s.workoutExercise we " +
            "JOIN we.workout w " +
            "WHERE w.user.id = :userId " +
            "AND we.exercise.id IN :exerciseIds " +
            "AND w.endTime IS NOT NULL " +
            "GROUP BY we.exercise.id, w.id, w.startTime")
    List<ExerciseWorkoutBest> findBestPerWorkout(@Param("userId") Long userId,
                                                 @Param("exerciseIds") Collection<Long> exerciseIds);

    @Query("SELECT new com.marosirobi.workouttrackerapi.dto.exercise.ExerciseSessionStat(" +
            "w.id, w.startTime, " +
            "CAST(MAX(s.weight) AS DOUBLE), " +
            "CAST(MAX(CASE WHEN s.reps = 1 " +
            "THEN s.weight + :bodyWeightFactor * COALESCE(w.bodyWeightKg, 0) " +
            "ELSE (s.weight + :bodyWeightFactor * COALESCE(w.bodyWeightKg, 0)) * (1 + s.reps / 30.0) END) AS DOUBLE), " +
            "CAST(MAX((s.weight + :bodyWeightFactor * COALESCE(w.bodyWeightKg, 0)) * s.reps) AS DOUBLE), " +
            "CAST(SUM((s.weight + :bodyWeightFactor * COALESCE(w.bodyWeightKg, 0)) * s.reps) AS DOUBLE), " +
            "CAST(MAX(s.reps) AS INTEGER), " +
            "CAST(MAX(s.durationSeconds) AS INTEGER)) " +
            "FROM ExerciseSet s " +
            "JOIN s.workoutExercise we " +
            "JOIN we.workout w " +
            "WHERE w.user.id = :userId " +
            "AND we.exercise.id = :exerciseId " +
            "AND w.endTime IS NOT NULL " +
            "GROUP BY w.id, w.startTime " +
            "ORDER BY w.startTime ASC")
    List<ExerciseSessionStat> findSessionStats(@Param("userId") Long userId,
                                               @Param("exerciseId") Long exerciseId,
                                               @Param("bodyWeightFactor") double bodyWeightFactor);
}
