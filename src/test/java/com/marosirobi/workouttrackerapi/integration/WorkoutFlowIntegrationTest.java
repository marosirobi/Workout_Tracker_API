package com.marosirobi.workouttrackerapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutFlowIntegrationTest extends IntegrationTestSupport {

    private void setBodyWeight(TestUser user, double kg) {
        assertThat(mvc.put().uri("/api/users/me")
                .header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","bodyWeightKg":%s}
                        """.formatted(user.username(), user.email(), kg)))
                .hasStatusOk();
    }

    private long exerciseId(TestUser user, String name) throws Exception {
        MvcTestResult result = mvc.get().uri("/api/exercises").header("Authorization", user.bearer()).exchange();
        List<Number> ids = read(result, "$[?(@.exerciseName == '" + name + "')].id");
        return ids.getFirst().longValue();
    }

    private MvcTestResult startWorkout(TestUser user, String type) {
        return mvc.post().uri("/api/workouts")
                .header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"%s","startTime":"%s"}
                        """.formatted(type, Instant.now().minus(1, ChronoUnit.HOURS)))
                .exchange();
    }

    private long startStrengthWorkout(TestUser user) throws Exception {
        MvcTestResult result = startWorkout(user, "STRENGTH");
        assertThat(result).hasStatus(HttpStatus.CREATED);
        Number id = read(result, "$.id");
        return id.longValue();
    }

    @Test
    void strengthWorkout_fullLifecycle() throws Exception {
        var user = registerUser();
        setBodyWeight(user, 80);
        long benchId = exerciseId(user, "Bench press");
        long workoutId = startStrengthWorkout(user);

        assertThat(mvc.put().uri("/api/workouts/{id}", workoutId)
                .header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"workoutTitle":"Push day","endTime":"%s","exercises":[
                          {"exerciseId":%d,"orderIndex":0,"sets":[{"setNumber":1,"weight":100.0,"reps":5}]}
                        ]}
                        """.formatted(Instant.now(), benchId)))
                .hasStatusOk();

        assertThat(mvc.get().uri("/api/workouts").header("Authorization", user.bearer()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.content[0].totalVolume").isEqualTo(500.0);

        assertThat(mvc.get().uri("/api/workouts/{id}", workoutId).header("Authorization", user.bearer()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.exercises[0].sets[0].weight").isEqualTo(100.0);
    }

    @Test
    void startingWithoutBodyWeight_returns400() throws Exception {
        var user = registerUser();

        assertThat(startWorkout(user, "STRENGTH")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void otherUser_cannotReadOrDeleteWorkout() throws Exception {
        var owner = registerUser();
        var stranger = registerUser();
        setBodyWeight(owner, 80);
        long workoutId = startStrengthWorkout(owner);

        assertThat(mvc.get().uri("/api/workouts/{id}", workoutId).header("Authorization", stranger.bearer()))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/workouts/{id}", workoutId).header("Authorization", stranger.bearer()))
                .hasStatus(HttpStatus.NOT_FOUND);

        assertThat(mvc.delete().uri("/api/workouts/{id}", workoutId).header("Authorization", owner.bearer()))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/workouts/{id}", workoutId).header("Authorization", owner.bearer()))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void range_withReversedInterval_returns400() throws Exception {
        var user = registerUser();
        Instant now = Instant.now();

        assertThat(mvc.get().uri("/api/workouts/range")
                .queryParam("from", now.toString())
                .queryParam("to", now.minus(1, ChronoUnit.DAYS).toString())
                .header("Authorization", user.bearer()))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
