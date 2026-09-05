package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.CardioStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardioStatsRepository extends JpaRepository<CardioStats, Long> {
    Optional<CardioStats> findByWorkoutId(Long workoutId);
}
