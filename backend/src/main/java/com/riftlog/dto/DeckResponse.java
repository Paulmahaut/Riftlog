package com.riftlog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.riftlog.entity.DeckZone;

public record DeckResponse(
        UUID id,
        Long ownerId,
        String ownerName,
        String name,
        CardRef legend,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<DeckCardResponse> cards
) {

    public record DeckCardResponse(
            CardRef card,
            DeckZone zone,
            int quantity
    ) {
    }
}
