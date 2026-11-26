package com.riftlog.dto;

import java.util.List;

import com.riftlog.entity.DeckZone;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Full deck content. The card list is optional: a deck can be just a name and a legend. */
public record DeckRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull Long legendCardId,
        @Size(max = 2000) String notes,
        List<@Valid DeckCardRequest> cards
) {

    public record DeckCardRequest(
            @NotNull Long cardId,
            @NotNull DeckZone zone,
            @Min(1) @Max(99) int quantity
    ) {
    }
}
