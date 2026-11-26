package com.riftlog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.riftlog.entity.GameEndReason;
import com.riftlog.entity.GameResult;
import com.riftlog.entity.MatchFormat;
import com.riftlog.entity.MatchSource;
import com.riftlog.entity.ScoreType;

/**
 * A match as returned by the API. In list views {@code games} is empty;
 * {@code gameWins} and the per-game scores are always derived, never typed in.
 */
public record MatchResponse(
        UUID id,
        MatchFormat format,
        MatchSource source,
        String externalId,
        Instant playedAt,
        Long recordedById,
        Integer winnerSeat,
        List<Integer> gameWins,
        int gamesPlayed,
        List<PlayerView> players,
        List<GameView> games
) {

    public record PlayerView(
            int seat,
            Long userId,
            Long guestId,
            String name,
            CardRef legend,
            UUID deckId,
            String deckName
    ) {
    }

    public record GameView(
            UUID id,
            int number,
            int firstSeat,
            Instant startedAt,
            Instant endedAt,
            Long durationSeconds,
            GameResult result,
            GameEndReason endReason,
            List<GamePlayerView> players,
            List<SideChangeView> sideChanges,
            List<EventView> events
    ) {
    }

    public record GamePlayerView(
            int seat,
            int finalScore,
            CardRef battlefield,
            Integer mulliganCount,
            String notes
    ) {
    }

    public record SideChangeView(
            int seat,
            CardRef card,
            int delta
    ) {
    }

    public record EventView(
            UUID id,
            int seq,
            int seat,
            ScoreType type,
            Instant occurredAt
    ) {
    }
}
