package com.marosirobi.workouttrackerapi.repository;

import com.marosirobi.workouttrackerapi.domain.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymRepository extends JpaRepository<Gym, Long> {

    List<Gym> findByUserId(Long userId);
}
