package com.marosirobi.workouttrackerapi.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "template_exercises")
@Getter
@Setter
@NoArgsConstructor
public class TemplateExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_sets")
    private Integer targetSets;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_template_id", nullable = false)
    private WorkoutTemplate workoutTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;


}
