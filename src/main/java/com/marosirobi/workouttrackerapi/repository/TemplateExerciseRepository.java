package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.TemplateExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateExerciseRepository extends JpaRepository<TemplateExercise, Long> {
}
