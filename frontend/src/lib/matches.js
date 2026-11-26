// Matches as read from the API (MatchResponse): helpers for the history, the detail page and stats.
import { api } from "./api.js";
import { session } from "./session.js";

export const POINT_LABEL = { CONQUER: "Conquer", HOLD: "Hold", ABILITY: "Ability" };
export const END_REASON_LABEL = { POINTS: "aux points", CONCEDE: "par abandon", DRAW: "égalité" };

export const fetchMatch = (id) => api(`/api/matches/${id}`);
export const deleteMatch = (id) => api(`/api/matches/${id}`, { method: "DELETE" });

/** Recorder or a registered player of the match: the server lets them change it. */
export function canEdit(match, userId = session.user?.id) {
    return match.recordedById === userId || match.players.some((player) => player.userId === userId);
}

/** 0-100, or null when nothing was played. Draws count as played, not won. */
export function winRate(tally) {
    return tally && tally.played > 0 ? Math.round((tally.wins / tally.played) * 100) : null;
}

/**
 * Rebuilds the upload body (MatchRequest) from a MatchResponse, with edited post-game details.
 * Every id is kept, so the PUT replaces the match instead of creating another one.
 *
 * @param {object} match MatchResponse with games
 * @param {Record<string, Record<number, { battlefieldId: number|null, mulligan: number|null, notes: string }>>} [details]
 *        per game id, per seat
 */
export function toRequest(match, details = {}) {
    return {
        format: match.format,
        source: match.source,
        externalId: match.externalId,
        playedAt: match.playedAt,
        players: match.players.map((player) => ({
            seat: player.seat,
            userId: player.userId,
            guestId: player.userId == null ? player.guestId : null,
            guestName: null,
            deckId: player.deckId,
            legendCardId: player.legend?.id ?? null,
        })),
        games: match.games.map((game) => ({
            id: game.id,
            number: game.number,
            firstSeat: game.firstSeat,
            startedAt: game.startedAt,
            endedAt: game.endedAt,
            result: game.result,
            endReason: game.endReason,
            players: game.players.map((player) => {
                const edited = details[game.id]?.[player.seat];
                return {
                    seat: player.seat,
                    finalScore: player.finalScore,
                    battlefieldCardId: edited ? edited.battlefieldId : (player.battlefield?.id ?? null),
                    mulliganCount: edited ? edited.mulligan : player.mulliganCount,
                    notes: edited ? edited.notes?.trim() || null : player.notes,
                };
            }),
            sideChanges: game.sideChanges.map((change) => ({ seat: change.seat, cardId: change.card.id, delta: change.delta })),
            events: game.events.map((event) => ({
                id: event.id,
                seq: event.seq,
                seat: event.seat,
                type: event.type,
                occurredAt: event.occurredAt,
            })),
        })),
    };
}

export const saveMatch = (match, details) =>
    api(`/api/matches/${match.id}`, { method: "PUT", body: toRequest(match, details) });

/** Running score after each point: [{ ...event, score: [seat1, seat2] }]. */
export function withRunningScore(events) {
    const score = [0, 0];
    return events.map((event) => {
        score[event.seat - 1]++;
        return { ...event, score: [...score] };
    });
}
