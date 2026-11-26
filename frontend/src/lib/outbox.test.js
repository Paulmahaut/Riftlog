import { IDBFactory } from "fake-indexeddb";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { closeDbForTests } from "./db.js";
import { enqueueMatch, flushOutbox, outboxItems, outboxState, retryItem } from "./outbox.js";
import { clearSession, session, setSession } from "./session.js";

const PAUL = 1;
const ALEX = 2;

function reply(status, body) {
    return Promise.resolve(new Response(body === undefined ? null : JSON.stringify(body), { status }));
}

describe("outbox", () => {
    let fetchMock;

    beforeEach(async () => {
        await closeDbForTests();
        globalThis.indexedDB = new IDBFactory();
        setSession("token-paul", { id: PAUL, displayName: "Paul" });
        fetchMock = vi.fn();
        vi.stubGlobal("fetch", fetchMock);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        clearSession();
    });

    it("uploads pending matches with PUT and forgets them once accepted", async () => {
        await enqueueMatch(PAUL, "m1", { format: "BO1" });
        await enqueueMatch(PAUL, "m2", { format: "BO3" });
        fetchMock.mockImplementation(() => reply(201, { id: "x" }));

        const result = await flushOutbox(PAUL);

        expect(result).toEqual({ sent: 2, rejected: 0, stoppedBy: null });
        expect(fetchMock).toHaveBeenCalledTimes(2);
        const [url, init] = fetchMock.mock.calls[0];
        expect(url).toBe("/api/matches/m1");
        expect(init.method).toBe("PUT");
        expect(init.headers.Authorization).toBe("Bearer token-paul");
        expect(await outboxItems(PAUL)).toEqual([]);
        expect(outboxState.pending).toBe(0);
    });

    it("keeps everything when the server cannot be reached", async () => {
        await enqueueMatch(PAUL, "m1", {});
        await enqueueMatch(PAUL, "m2", {});
        fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));

        const result = await flushOutbox(PAUL);

        expect(result.stoppedBy).toBe("offline");
        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect((await outboxItems(PAUL)).map((item) => item.status)).toEqual(["pending", "pending"]);
        expect(outboxState.pending).toBe(2);
    });

    it("marks a match the server refuses as rejected, with its reason, and stops retrying it", async () => {
        await enqueueMatch(PAUL, "bad", {});
        await enqueueMatch(PAUL, "good", {});
        fetchMock
            .mockImplementationOnce(() => reply(400, { message: "Game 1 was won on points but the score is 7-5" }))
            .mockImplementationOnce(() => reply(201, {}));

        const result = await flushOutbox(PAUL);
        const [bad] = await outboxItems(PAUL);

        expect(result).toEqual({ sent: 1, rejected: 1, stoppedBy: null });
        expect(bad.status).toBe("rejected");
        expect(bad.lastError).toContain("7-5");
        expect(outboxState.rejected).toBe(1);

        await flushOutbox(PAUL);
        expect(fetchMock).toHaveBeenCalledTimes(2);

        await retryItem(PAUL, "bad");
        expect(outboxState.pending).toBe(1);
    });

    it("stops and flags the session when the token is refused", async () => {
        await enqueueMatch(PAUL, "m1", {});
        fetchMock.mockImplementation(() => reply(401, { message: "expired" }));

        const result = await flushOutbox(PAUL);

        expect(result.stoppedBy).toBe("session");
        expect(session.expired).toBe(true);
        expect((await outboxItems(PAUL))[0].status).toBe("pending");
    });

    it("keeps a match pending after a server error, counting the attempt", async () => {
        await enqueueMatch(PAUL, "m1", {});
        fetchMock.mockImplementation(() => reply(500, { message: "Unexpected server error" }));

        await flushOutbox(PAUL);
        const [item] = await outboxItems(PAUL);

        expect(item.status).toBe("pending");
        expect(item.attempts).toBe(1);
    });

    it("only uploads the matches of the logged-in player", async () => {
        await enqueueMatch(ALEX, "alex-match", {});
        await enqueueMatch(PAUL, "paul-match", {});
        fetchMock.mockImplementation(() => reply(201, {}));

        await flushOutbox(PAUL);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(fetchMock.mock.calls[0][0]).toBe("/api/matches/paul-match");
        expect(await outboxItems(ALEX)).toHaveLength(1);
    });

    it("runs a single upload at a time even if triggered twice", async () => {
        await enqueueMatch(PAUL, "m1", {});
        fetchMock.mockImplementation(() => reply(201, {}));

        const [first, second] = await Promise.all([flushOutbox(PAUL), flushOutbox(PAUL)]);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(first).toBe(second);
    });
});
