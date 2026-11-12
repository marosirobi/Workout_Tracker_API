package com.marosirobi.workouttrackerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WorkouttrackerapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkouttrackerapiApplication.class, args);
    }

}
