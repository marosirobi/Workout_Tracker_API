package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    Optional<Workout> findByUserId(Long userId);
}
