package com.marosirobi.workouttrackerapi.domain.entity;

import com.marosirobi.workouttrackerapi.domain.enums.MuscleGroup;
import com.marosirobi.workouttrackerapi.domain.enums.TrackingType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise_name",nullable = false, unique = true)
    private String exerciseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false)
    private MuscleGroup muscleGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_type")
    private TrackingType trackingType;

    public TrackingType effectiveTrackingType() {
        return trackingType != null ? trackingType : TrackingType.WEIGHT_REPS;
    }
}
