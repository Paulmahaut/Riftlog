// The match being played at the table. Pure functions mutate a plain "live match" object,
// which the duel screen saves to IndexedDB after every change: closing the app, a phone call
// or a reload never loses a point. When the match is over it becomes a MatchRequest in the outbox.
//
// Seats: 1 = the phone owner (bottom half), 2 = the opponent (top half, rotated).
import { db } from "./db.js";
import { enqueueMatch } from "./outbox.js";

export const WINNING_SCORE = 8;
export const POINT_TYPES = ["CONQUER", "HOLD", "ABILITY"];
export const FORMATS = {
    BO1: { gamesToWin: 1, maxGames: 1 },
    BO3: { gamesToWin: 2, maxGames: 3 },
};

const LIVE_KEY = "live-match";
/** crypto.randomUUID only exists on https/localhost; a phone testing over local Wi-Fi (http) gets the fallback. */
function uuid() {
    if (crypto.randomUUID) return crypto.randomUUID();
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40; // version 4
    bytes[8] = (bytes[8] & 0x3f) | 0x80; // variant
    const hex = [...bytes].map((b) => b.toString(16).padStart(2, "0")).join("");
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/**
 * @param {object} options
 * @param {number} options.userId the phone owner, who records the match
 * @param {"BO1"|"BO3"} options.format
 * @param {{ kind: "user", id: number, name: string, legendId: number, legendName: string }} options.me
 * @param {{ kind: "user"|"guest"|"new-guest", id: number|null, name: string, legendId: number, legendName: string }} options.opponent
 */
export function newMatch({ userId, format, me, opponent, now = Date.now() }) {
    return {
        key: LIVE_KEY,
        id: uuid(),
        userId,
        format,
        createdAt: now,
        players: [
            { seat: 1, ...me },
            { seat: 2, ...opponent },
        ],
        games: [newGame(1)],
        phase: "pick-first", // pick-first | playing | game-over | match-over
    };
}

function newGame(number) {
    return {
        id: uuid(),
        number,
        firstSeat: null,
        startedAt: null,
        endedAt: null,
        result: null, // SEAT1 | SEAT2 | DRAW
        endReason: null, // POINTS | CONCEDE | DRAW
        events: [], // { id, seat, type, at }
        details: { 1: { battlefieldId: null, mulligan: null }, 2: { battlefieldId: null, mulligan: null } },
    };
}

export const currentGame = (match) => match.games[match.games.length - 1];
export const score = (game, seat) => game.events.filter((event) => event.seat === seat).length;
export const winsOf = (match, seat) => match.games.filter((game) => game.result === `SEAT${seat}`).length;
export const otherSeat = (seat) => 3 - seat;
export const player = (match, seat) => match.players[seat - 1];

/** The format can still change until the first point is scored. */
export const canChangeFormat = (match) => match.games.length === 1 && currentGame(match).events.length === 0;

export function setFormat(match, format) {
    if (canChangeFormat(match) && FORMATS[format]) {
        match.format = format;
    }
}

export function pickFirst(match, seat, now = Date.now()) {
    if (match.phase !== "pick-first") return;
    const game = currentGame(match);
    game.firstSeat = seat;
    game.startedAt = now;
    match.phase = "playing";
}

/** Adds a point; reaching 8 ends the game. Returns true when it did. */
export function addPoint(match, seat, type, now = Date.now()) {
    if (match.phase !== "playing" || !POINT_TYPES.includes(type)) return false;
    const game = currentGame(match);
    game.events.push({ id: uuid(), seat, type, at: now });
    if (score(game, seat) >= WINNING_SCORE) {
        endGame(match, `SEAT${seat}`, "POINTS", now);
        return true;
    }
    return false;
}

/** result: SEAT1 | SEAT2 | DRAW. reason: POINTS | CONCEDE | DRAW. */
export function endGame(match, result, reason, now = Date.now()) {
    if (match.phase !== "playing") return;
    const game = currentGame(match);
    game.result = result;
    game.endReason = reason;
    game.endedAt = now;
    match.phase = "game-over";
}

/** Concede: the player on `seat` gives up the current game. */
export const concede = (match, seat, now) => endGame(match, `SEAT${otherSeat(seat)}`, "CONCEDE", now);

/** Removes the last point; on a finished game, reopens it (removing the winning point if it ended on points). */
export function undo(match) {
    const game = currentGame(match);
    if (match.phase === "game-over") {
        if (game.endReason === "POINTS") game.events.pop();
        game.result = null;
        game.endReason = null;
        game.endedAt = null;
        match.phase = "playing";
        return true;
    }
    if (match.phase === "playing" && game.events.length > 0) {
        game.events.pop();
        return true;
    }
    return false;
}

export const canUndo = (match) =>
    match.phase === "game-over" || (match.phase === "playing" && currentGame(match).events.length > 0);

/** True when no more game will be played: someone has enough wins, or all games are used. */
export function isDecided(match) {
    const { gamesToWin, maxGames } = FORMATS[match.format];
    return winsOf(match, 1) >= gamesToWin || winsOf(match, 2) >= gamesToWin || match.games.length >= maxGames;
}

/** After a finished game: starts the next one, or ends the match. */
export function next(match) {
    if (match.phase !== "game-over") return;
    if (isDecided(match)) {
        match.phase = "match-over";
    } else {
        match.games.push(newGame(match.games.length + 1));
        match.phase = "pick-first";
    }
}

/** 1, 2, or null for a drawn match. */
export function matchWinnerSeat(match) {
    const [one, two] = [winsOf(match, 1), winsOf(match, 2)];
    if (one === two) return null;
    return one > two ? 1 : 2;
}

const iso = (ms) => (ms == null ? null : new Date(ms).toISOString());

/** The body of PUT /api/matches/{id}. */
export function toMatchRequest(match) {
    return {
        format: match.format,
        source: "LIVE",
        playedAt: iso(currentGame(match).endedAt ?? match.games[0].startedAt ?? match.createdAt),
        players: match.players.map((slot) => ({
            seat: slot.seat,
            userId: slot.kind === "user" ? slot.id : null,
            guestId: slot.kind === "guest" ? slot.id : null,
            guestName: slot.kind === "new-guest" ? slot.name : null,
            legendCardId: slot.legendId,
        })),
        games: match.games
            .filter((game) => game.result)
            .map((game) => ({
                id: game.id,
                number: game.number,
                firstSeat: game.firstSeat,
                startedAt: iso(game.startedAt),
                endedAt: iso(game.endedAt),
                result: game.result,
                endReason: game.endReason,
                players: [1, 2].map((seat) => ({
                    seat,
                    battlefieldCardId: game.details[seat].battlefieldId,
                    mulliganCount: game.details[seat].mulligan,
                })),
                sideChanges: [],
                events: game.events.map((event, index) => ({
                    id: event.id,
                    seq: index + 1,
                    seat: event.seat,
                    type: event.type,
                    occurredAt: iso(event.at),
                })),
            })),
    };
}

// ------------------------------------------------------------------
// Persistence: one live match per device
// ------------------------------------------------------------------

/** The match in progress on this device for this user, or null. */
export async function loadLiveMatch(userId) {
    const match = await (await db()).get("meta", LIVE_KEY);
    return match && match.userId === userId ? match : null;
}

export async function saveLiveMatch(match) {
    // JSON round-trip: IndexedDB cannot store Vue's reactive proxies.
    await (await db()).put("meta", JSON.parse(JSON.stringify(match)));
}

export async function clearLiveMatch() {
    await (await db()).delete("meta", LIVE_KEY);
}

/** Puts the finished match in the outbox. Safe to call again after editing post-game details. */
export async function recordMatch(match) {
    await enqueueMatch(match.userId, match.id, toMatchRequest(match));
}
