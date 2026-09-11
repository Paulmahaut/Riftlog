package com.riftlog.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateMatchRequest(
        @NotBlank String opponentName,
        @NotBlank String myLegendName,
        @NotBlank String myDeckName,
        @NotBlank String opponentLegendName,
        @NotBlank String opponentDeckName,
        @NotEmpty List<@Valid RoundRequest> rounds
) {
}
