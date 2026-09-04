package com.marosirobi.workouttrackerapi.controller;

import com.marosirobi.workouttrackerapi.dto.stat.PersonalRecordResponse;
import com.marosirobi.workouttrackerapi.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/pr/{exerciseId}")
    public PersonalRecordResponse getPersonalRecord(@PathVariable Long exerciseId, Principal principal){
        return statService.getPersonalRecord(exerciseId, principal.getName());
    }
}
