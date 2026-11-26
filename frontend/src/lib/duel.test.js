import { IDBFactory } from "fake-indexeddb";
import { beforeEach, describe, expect, it } from "vitest";
import { closeDbForTests } from "./db.js";
import {
    addPoint,
    canChangeFormat,
    clearLiveMatch,
    concede,
    endGame,
    isDecided,
    loadLiveMatch,
    matchWinnerSeat,
    newMatch,
    next,
    pickFirst,
    saveLiveMatch,
    score,
    toMatchRequest,
    undo,
    currentGame,
} from "./duel.js";

const ME = { kind: "user", id: 1, name: "Paul", legendId: 10, legendName: "Jinx - Loose Cannon" };
const GUEST = { kind: "new-guest", id: null, name: "Thomas", legendId: 20, legendName: "Vi - Piltover Enforcer" };

const start = (format = "BO3") => newMatch({ userId: 1, format, me: ME, opponent: GUEST, now: 1000 });

/** Scores `points` for `seat`, one second apart. */
function scoreFor(match, seat, points, from = 2000) {
    for (let i = 0; i < points; i++) addPoint(match, seat, "CONQUER", from + i * 1000);
}

describe("duel", () => {
    it("ignores points until someone is picked to go first", () => {
        const match = start();
        addPoint(match, 1, "HOLD");
        expect(currentGame(match).events).toHaveLength(0);

        pickFirst(match, 2, 1500);
        addPoint(match, 1, "HOLD", 1600);
        expect(score(currentGame(match), 1)).toBe(1);
        expect(currentGame(match).firstSeat).toBe(2);
    });

    it("ends the game at 8 points, and undo reopens it without the winning point", () => {
        const match = start();
        pickFirst(match, 1);
        scoreFor(match, 2, 7);
        expect(match.phase).toBe("playing");
        expect(addPoint(match, 2, "ABILITY")).toBe(true);
        expect(match.phase).toBe("game-over");
        expect(currentGame(match).result).toBe("SEAT2");

        undo(match);
        expect(match.phase).toBe("playing");
        expect(score(currentGame(match), 2)).toBe(7);
        expect(currentGame(match).result).toBeNull();
    });

    it("plays a Bo3 to 2 wins, with the loser picking who starts the next game", () => {
        const match = start();
        pickFirst(match, 1);
        scoreFor(match, 1, 8);
        next(match);
        expect(match.phase).toBe("pick-first");
        expect(match.games).toHaveLength(2);

        pickFirst(match, 2);
        concede(match, 2);
        expect(currentGame(match).result).toBe("SEAT1");
        expect(isDecided(match)).toBe(true);
        next(match);
        expect(match.phase).toBe("match-over");
        expect(matchWinnerSeat(match)).toBe(1);
    });

    it("stops a Bo3 after 3 games even when a draw leaves it 1-1", () => {
        const match = start();
        for (const result of ["DRAW", "SEAT1", "SEAT2"]) {
            pickFirst(match, 1);
            endGame(match, result, result === "DRAW" ? "DRAW" : "CONCEDE");
            next(match);
        }
        expect(match.phase).toBe("match-over");
        expect(matchWinnerSeat(match)).toBeNull();
    });

    it("locks the format once a point is scored", () => {
        const match = start("BO1");
        expect(canChangeFormat(match)).toBe(true);
        pickFirst(match, 1);
        addPoint(match, 1, "HOLD");
        expect(canChangeFormat(match)).toBe(false);
    });

    it("builds the request the server expects", () => {
        const match = start("BO1");
        pickFirst(match, 1, 1500);
        addPoint(match, 2, "HOLD", 2000);
        scoreFor(match, 1, 8, 3000);
        currentGame(match).details[1] = { battlefieldId: 55, mulligan: 1 };
        next(match);

        const body = toMatchRequest(match);
        expect(body.source).toBe("LIVE");
        expect(body.players).toEqual([
            { seat: 1, userId: 1, guestId: null, guestName: null, legendCardId: 10 },
            { seat: 2, userId: null, guestId: null, guestName: "Thomas", legendCardId: 20 },
        ]);
        const [game] = body.games;
        expect(game).toMatchObject({ number: 1, firstSeat: 1, result: "SEAT1", endReason: "POINTS" });
        expect(game.events.map((event) => event.seq)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9]);
        expect(game.events[0]).toMatchObject({ seat: 2, type: "HOLD", occurredAt: new Date(2000).toISOString() });
        expect(game.players[0]).toEqual({ seat: 1, battlefieldCardId: 55, mulliganCount: 1 });
        expect(body.playedAt).toBe(game.endedAt);
    });

    it("survives a reload, and is only resumed by the player who started it", async () => {
        await closeDbForTests();
        globalThis.indexedDB = new IDBFactory();
        const match = start();
        pickFirst(match, 1);
        addPoint(match, 1, "CONQUER");
        await saveLiveMatch(match);

        expect(await loadLiveMatch(2)).toBeNull();
        const loaded = await loadLiveMatch(1);
        expect(score(currentGame(loaded), 1)).toBe(1);

        await clearLiveMatch();
        expect(await loadLiveMatch(1)).toBeNull();
    });
});
