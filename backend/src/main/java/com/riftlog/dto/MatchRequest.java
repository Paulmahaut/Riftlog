package com.riftlog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.riftlog.entity.GameEndReason;
import com.riftlog.entity.GameResult;
import com.riftlog.entity.MatchFormat;
import com.riftlog.entity.MatchSource;
import com.riftlog.entity.ScoreType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A whole match, sent once it is over (PUT /api/matches/{id}).
 * Sending the same match again replaces it, so an upload retried after a network drop
 * never creates a duplicate, and post-game details can be completed later from the PC.
 */
public record MatchRequest(
        @NotNull MatchFormat format,
        @NotNull MatchSource source,
        @Size(max = 255) String externalId,
        @NotNull Instant playedAt,
        @NotNull @Size(min = 2, max = 2) List<@Valid @NotNull PlayerSlot> players,
        @NotEmpty @Size(max = 3) List<@Valid @NotNull GameData> games
) {

    /**
     * Who sits on a seat: exactly one of userId, guestId or guestName.
     * guestName lets an offline phone add a new guest; the server finds or creates it.
     * legendCardId may be omitted when deckId is given.
     */
    public record PlayerSlot(
            @Min(1) @Max(2) int seat,
            Long userId,
            Long guestId,
            @Size(max = 100) String guestName,
            UUID deckId,
            Long legendCardId
    ) {
    }

    public record GameData(
            @NotNull UUID id,
            @Min(1) @Max(3) int number,
            @Min(1) @Max(2) int firstSeat,
            Instant startedAt,
            Instant endedAt,
            @NotNull GameResult result,
            GameEndReason endReason,
            List<@Valid @NotNull GamePlayerData> players,
            List<@Valid @NotNull SideChangeData> sideChanges,
            List<@Valid @NotNull EventData> events
    ) {
    }

    /** Post-game details for one seat. finalScore is only read for MANUAL/IMPORTED matches. */
    public record GamePlayerData(
            @Min(1) @Max(2) int seat,
            @Min(0) @Max(99) Integer finalScore,
            Long battlefieldCardId,
            @Min(0) @Max(2) Integer mulliganCount,
            @Size(max = 2000) String notes
    ) {
    }

    public record SideChangeData(
            @Min(1) @Max(2) int seat,
            @NotNull Long cardId,
            @Min(-99) @Max(99) int delta
    ) {
    }

    public record EventData(
            @NotNull UUID id,
            @Min(1) int seq,
            @Min(1) @Max(2) int seat,
            @NotNull ScoreType type,
            @NotNull Instant occurredAt
    ) {
    }
}
