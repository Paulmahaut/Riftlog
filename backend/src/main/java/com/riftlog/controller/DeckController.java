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

import com.riftlog.dto.DeckRequest;
import com.riftlog.dto.DeckResponse;
import com.riftlog.dto.Saved;
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
    public List<DeckResponse> list(@RequestParam(required = false) Long ownerId) {
        return deckService.list(ownerId);
    }

    @GetMapping("/{id}")
    public DeckResponse get(@PathVariable UUID id) {
        return deckService.get(id);
    }

    /** The client picks the id (UUID), so a deck created offline keeps it once synced. */
    @PutMapping("/{id}")
    public ResponseEntity<DeckResponse> upsert(@PathVariable UUID id, @Valid @RequestBody DeckRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        Saved<DeckResponse> saved = deckService.upsert(id, request, currentUser.id());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.value());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        deckService.delete(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
