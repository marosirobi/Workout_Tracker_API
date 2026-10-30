package com.marosirobi.workouttrackerapi.service;

import com.marosirobi.workouttrackerapi.domain.entity.*;
import com.marosirobi.workouttrackerapi.dto.stat.PersonalRecordResponse;
import com.marosirobi.workouttrackerapi.exception.ResourceNotFoundException;
import com.marosirobi.workouttrackerapi.repository.BestEffortRepository;
import com.marosirobi.workouttrackerapi.repository.ExerciseRepository;
import com.marosirobi.workouttrackerapi.repository.ExerciseSetRepository;
import com.marosirobi.workouttrackerapi.repository.UserRepository;
import com.marosirobi.workouttrackerapi.utils.GeoUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatService {

    private final ExerciseSetRepository exerciseSetRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final BestEffortRepository bestEffortRepository;

    private static final List<Integer> TARGET_DISTANCES =
            List.of(400, 1000, 1609, 5000, 10000, 21098, 42195);

    private static final double REALISTIC_MAX_SPEED_MS = 20.0;

    @Transactional
    public void calculateAndSave(Workout workout, List<TrackPoint> points){
        List<BestEffort> efforts = calculate(workout,points);
        bestEffortRepository.saveAll(efforts);
        workout.getBestEfforts().addAll(efforts);
    }

    List<BestEffort> calculate(Workout workout, List<TrackPoint> rawPoints){
        if(rawPoints == null || rawPoints.size() < 2){
            return List.of();
        }

        List<TrackPoint> points = rawPoints.stream()
                .sorted(Comparator.comparing(TrackPoint::getTimestamp))
                .toList();
        int n = points.size();

        double[] cumMeters = new double[n];
        double[] cumSeconds = new double[n];

        for(int i = 1; i < n; i++){
            TrackPoint a = points.get(i-1);
            TrackPoint b = points.get(i);
            cumMeters[i] = cumMeters[i-1];
            cumSeconds[i] = cumSeconds[i-1];

            if(!sameSegment(a,b)) continue;

            double dt = Duration.between(a.getTimestamp(), b.getTimestamp()).toMillis() / 1000.0;
            if(dt <= 0) continue;

            cumMeters[i] += GeoUtils.distanceMeters(
                    a.getLatitude(),a.getLongitude(),b.getLatitude(),b.getLongitude());
            cumSeconds[i] += dt;
        }

        double totalMeters = cumMeters[n-1];
        List<BestEffort> result = new ArrayList<>();

        for(int target: TARGET_DISTANCES){
            if(totalMeters < target) break;

            double bestDuration = Double.MAX_VALUE;
            double bestStartMeters = 0;
            int end = 0;

            for(int start = 0; start < n; start++){
                double goal = cumMeters[start] + target;

                if(goal > totalMeters) break;
                if(end < start) end = start;

                while(cumMeters[end] < goal) end++;

                double stepMeters = cumMeters[end] - cumMeters[end - 1];
                double fraction = stepMeters > 0 ? (goal - cumMeters[end - 1]) / stepMeters : 0;
                double reachedAt = cumSeconds[end - 1] + fraction * (cumSeconds[end] - cumSeconds[end-1]);

                double duration = reachedAt - cumSeconds[start];
                boolean realistic = duration > 0 && target / duration <= REALISTIC_MAX_SPEED_MS;

                if(realistic && duration < bestDuration){
                    bestDuration = duration;
                    bestStartMeters = cumMeters[start];
                }
            }

            if(bestDuration < Double.MAX_VALUE){
                result.add(BestEffort.builder()
                        .workout(workout)
                        .distanceMeters(target)
                        .durationSeconds(Math.round(bestDuration * 10) / 10.0)
                        .startOffsetMeters(Math.round(bestStartMeters * 10) / 10.0)
                        .build());
            }
        }
        return result;
    }

    private static boolean sameSegment(TrackPoint a, TrackPoint b) {
        int segA = a.getSegment() != null ? a.getSegment() : 0;
        int segB = b.getSegment() != null ? b.getSegment() : 0;
        return segA == segB;
    }

    public PersonalRecordResponse getPersonalRecord(Long exerciseId, String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found " + email));

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found " + exerciseId));

        Double maxWeight = exerciseSetRepository.findPersonalRecord(user.getId(), exercise.getId());

        //map to personal record response
        //later we will need other stats so i wont make a generic mapToResponse func
        return PersonalRecordResponse.builder()
                .exerciseId(exercise.getId())
                .maxWeight(maxWeight)
                .exerciseName(exercise.getExerciseName())
                .build();
    }
}
