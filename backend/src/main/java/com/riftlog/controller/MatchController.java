package com.riftlog.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.riftlog.dto.MatchRequest;
import com.riftlog.dto.MatchResponse;
import com.riftlog.dto.Saved;
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

    /** Summaries without games. ?userId= keeps only one player's matches. */
    @GetMapping
    public List<MatchResponse> list(@RequestParam(required = false) Long userId) {
        return matchService.list(userId);
    }

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable UUID id) {
        return matchService.get(id);
    }

    /**
     * Uploads a finished match under the id the phone generated.
     * 201 the first time, 200 when it replaces an earlier upload of the same match.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MatchResponse> upsert(@PathVariable UUID id, @Valid @RequestBody MatchRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Saved<MatchResponse> saved = matchService.upsert(id, request, currentUser.id());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.value());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        matchService.delete(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
