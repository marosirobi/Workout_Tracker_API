package com.marosirobi.workouttrackerapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "cardio_stats")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardioStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_distance", nullable = false)
    private Double totalDistance;

    @Column(name = "avg_pace")
    private String avgPace;

    @Column(name = "elevation_gain")
    private Integer elevationGain;

    @Column(name = "moving_time_seconds")
    private Integer movingTimeSeconds;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false, unique = true)
    private Workout workout;
}
