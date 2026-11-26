import { describe, expect, it } from "vitest";
import { canEdit, toRequest, winRate, withRunningScore } from "./matches.js";

const MATCH = {
    id: "m1",
    format: "BO1",
    source: "LIVE",
    externalId: null,
    playedAt: "2026-11-26T18:00:00Z",
    recordedById: 1,
    players: [
        { seat: 1, userId: 1, guestId: null, name: "Paul", legend: { id: 10 }, deckId: null },
        { seat: 2, userId: null, guestId: 4, name: "Thomas", legend: { id: 20 }, deckId: null },
    ],
    games: [
        {
            id: "g1",
            number: 1,
            firstSeat: 2,
            startedAt: "2026-11-26T18:00:00Z",
            endedAt: "2026-11-26T18:20:00Z",
            result: "SEAT1",
            endReason: "CONCEDE",
            players: [
                { seat: 1, finalScore: 3, battlefield: { id: 55 }, mulliganCount: 1, notes: null },
                { seat: 2, finalScore: 1, battlefield: null, mulliganCount: null, notes: "tilt" },
            ],
            sideChanges: [{ seat: 1, card: { id: 99 }, delta: -1 }],
            events: [{ id: "e1", seq: 1, seat: 2, type: "HOLD", occurredAt: "2026-11-26T18:01:00Z" }],
        },
    ],
};

describe("matches", () => {
    it("rebuilds the upload body with the same ids, keeping untouched details", () => {
        const body = toRequest(MATCH);
        expect(body.players[1]).toEqual({ seat: 2, userId: null, guestId: 4, guestName: null, deckId: null, legendCardId: 20 });
        const [game] = body.games;
        expect(game.id).toBe("g1");
        expect(game.players[0]).toEqual({ seat: 1, finalScore: 3, battlefieldCardId: 55, mulliganCount: 1, notes: null });
        expect(game.sideChanges).toEqual([{ seat: 1, cardId: 99, delta: -1 }]);
        expect(game.events[0]).toMatchObject({ id: "e1", seq: 1, seat: 2, type: "HOLD" });
    });

    it("applies edited details, clearing a battlefield and trimming notes", () => {
        const body = toRequest(MATCH, { g1: { 1: { battlefieldId: null, mulligan: 2, notes: "  bon match " } } });
        expect(body.games[0].players[0]).toMatchObject({ battlefieldCardId: null, mulliganCount: 2, notes: "bon match" });
        expect(body.games[0].players[1].notes).toBe("tilt");
    });

    it("lets the recorder and registered players edit, nobody else", () => {
        expect(canEdit(MATCH, 1)).toBe(true);
        expect(canEdit(MATCH, 7)).toBe(false);
    });

    it("computes win rates and running scores", () => {
        expect(winRate({ played: 4, wins: 3 })).toBe(75);
        expect(winRate({ played: 0, wins: 0 })).toBeNull();
        expect(withRunningScore([{ seat: 1 }, { seat: 2 }, { seat: 1 }]).map((e) => e.score)).toEqual([
            [1, 0],
            [1, 1],
            [2, 1],
        ]);
    });
});
