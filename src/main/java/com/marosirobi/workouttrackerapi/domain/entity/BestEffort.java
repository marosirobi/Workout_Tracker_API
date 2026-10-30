package com.marosirobi.workouttrackerapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Table(name = "best_efforts", indexes = {
        @Index(name = "id_best_effort_distance", columnList = "distance_meters")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BestEffort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private Double durationSeconds;

    @Column(name = "start_offset_meters", nullable = false)
    private Double startOffsetMeters;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;
}
