package com.riftlog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One player's statistics, always computed from the stored games and points (nothing is cached).
 * Counts are in games unless the name says otherwise; win rates are left to the client.
 */
public record StatsResponse(
        PlayerSummary player,
        Tally matches,
        Tally games,
        /** Games where the player went first, and where the opponent did. */
        Tally goingFirst,
        Tally goingSecond,
        Long averageGameSeconds,
        List<LegendLine> myLegends,
        List<LegendLine> opponentLegends,
        /** Only games counted point by point (live). */
        PointSplit pointsScored,
        PointSplit pointsConceded,
        List<BattlefieldLine> myBattlefields,
        List<BattlefieldLine> opponentBattlefields,
        List<Comeback> comebacks
) {

    public record Tally(int played, int wins, int losses, int draws) {
    }

    /** Results with or against one legend, split by who went first. */
    public record LegendLine(CardRef legend, Tally all, Tally goingFirst, Tally goingSecond) {
    }

    public record PointSplit(int conquer, int hold, int ability, int games) {
    }

    public record BattlefieldLine(CardRef battlefield, Tally games) {
    }

    /** A game won after being behind by at least {@code StatsService.COMEBACK_DEFICIT} points. */
    public record Comeback(
            UUID matchId,
            int gameNumber,
            Instant playedAt,
            String opponentName,
            CardRef opponentLegend,
            int scoreWhenBehind,
            int opponentScoreWhenBehind,
            int finalScore,
            int opponentFinalScore
    ) {
    }
}
