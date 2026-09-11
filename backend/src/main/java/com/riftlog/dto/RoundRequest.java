package com.riftlog.dto;

import jakarta.validation.constraints.Min;

public record RoundRequest(
        @Min(1) int roundNumber,
        @Min(0) int myScore,
        @Min(0) int opponentScore
) {
}
