package com.riftlog.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riftlog.dto.StatsResponse;
import com.riftlog.security.AuthenticatedUser;
import com.riftlog.service.StatsService;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsResponse getStats(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return statsService.computeStats(currentUser.id());
    }
}
