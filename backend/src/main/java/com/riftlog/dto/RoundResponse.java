package com.riftlog.dto;

public record RoundResponse(
        Long id,
        int roundNumber,
        int myScore,
        int opponentScore
) {
}
