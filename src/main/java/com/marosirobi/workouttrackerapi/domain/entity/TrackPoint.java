package com.marosirobi.workouttrackerapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "track_points")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrackPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double elevation;

    @Column(nullable = false)
    private Double speed;

    @Column(nullable = false)
    private Instant timestamp;

    @Column
    private Integer segment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;
}
