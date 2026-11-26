package com.riftlog.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** A player's statistics; everyone in the group can see everyone's. Defaults to the caller. */
    @GetMapping
    public StatsResponse get(@RequestParam(required = false) Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return statsService.forUser(userId == null ? currentUser.id() : userId);
    }
}
