package com.riftlog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riftlog.dto.DeckRequest;
import com.riftlog.dto.DeckResponse;
import com.riftlog.security.AuthenticatedUser;
import com.riftlog.service.DeckService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public List<DeckResponse> listAll(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return deckService.listAll(currentUser.id());
    }

    @PostMapping
    public ResponseEntity<DeckResponse> create(@Valid @RequestBody DeckRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deckService.create(request, currentUser.id()));
    }
}
