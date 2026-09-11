package com.riftlog.dto;

import jakarta.validation.constraints.NotBlank;

public record DeckRequest(
        @NotBlank String name,
        @NotBlank String legendName
) {
}
