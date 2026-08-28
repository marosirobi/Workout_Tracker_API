package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.Workout;
import com.marosirobi.workouttrackerapi.domain.entity.WorkoutTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {

    Optional<WorkoutTemplate> findByUserId(Long userId);
}
