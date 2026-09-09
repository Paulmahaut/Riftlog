package com.riftlog.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MatchResponse(
        Long id,
        LocalDateTime playedAt,
        String opponentName,
        String myLegendName,
        String myDeckName,
        String opponentLegendName,
        String opponentDeckName,
        String result,
        int myFinalScore,
        int opponentFinalScore,
        List<RoundResponse> rounds
) {
}
