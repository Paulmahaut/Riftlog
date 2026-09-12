package com.riftlog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.riftlog.dto.CreateMatchRequest;
import com.riftlog.dto.MatchResponse;
import com.riftlog.security.AuthenticatedUser;
import com.riftlog.service.MatchService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchResponse> logMatch(@Valid @RequestBody CreateMatchRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.logMatch(request, currentUser.id()));
    }

    @GetMapping
    public List<MatchResponse> listMatches(@RequestParam(required = false) Long opponentId,
                                            @RequestParam(required = false) Long deckId,
                                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return matchService.listMatches(opponentId, deckId, currentUser.id());
    }

    @GetMapping("/{id}")
    public MatchResponse getMatch(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return matchService.getMatch(id, currentUser.id());
    }
}
