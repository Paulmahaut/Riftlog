package com.riftlog.dto;

import java.util.Map;

public record StatsResponse(
        long totalMatches,
        double overallWinRate,
        Map<String, Double> winRateByDeck,
        Map<String, Double> winRateByMatchup
) {
}
