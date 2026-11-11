package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.Workout;
import com.marosirobi.workouttrackerapi.domain.enums.WorkoutType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    Page<Workout> findByUserIdAndEndTimeIsNotNull(Long userId, Pageable pageable);
    Page<Workout> findByUserIdAndTypeAndEndTimeIsNotNull(Long userId, Pageable pageable, WorkoutType type);
    Optional<Workout> findByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndUserId(Long id, Long userId);
    List<Workout> findByUserIdAndEndTimeIsNotNullAndStartTimeBetweenOrderByStartTimeAsc(
            Long userId, Instant from, Instant to);
}
