package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.BestEffort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BestEffortRepository extends JpaRepository<BestEffort, Long> {
    long countByWorkoutUserIdAndDistanceMetersAndDurationSecondsLessThan(Long userId, Integer distanceMeters, Double durationSeconds);
}
