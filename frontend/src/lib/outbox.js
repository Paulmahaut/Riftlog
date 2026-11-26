// Finished matches waiting to be uploaded. A match is written here first, then sent with
// PUT /api/matches/{id}. Because the id is generated on the phone, sending twice is harmless:
// the server replaces instead of duplicating, so a flaky connection can never create doubles.
import { reactive } from "vue";
import { api, ApiError, NetworkError } from "./api.js";
import { db } from "./db.js";

/** Live counters for the UI (badge, banners). Refreshed by every outbox operation. */
export const outboxState = reactive({
    pending: 0,
    rejected: 0,
    syncing: false,
});

let flushing = null;

/**
 * Stores a finished match for upload.
 * @param {number} userId who recorded it: only their session will upload it
 * @param {string} id match UUID
 * @param {object} body the MatchRequest JSON
 */
export async function enqueueMatch(userId, id, body) {
    await (await db()).put("outbox", {
        id,
        userId,
        body,
        status: "pending", // pending | rejected
        createdAt: new Date().toISOString(),
        attempts: 0,
        lastError: null,
    });
    await refreshOutboxState(userId);
}

export async function outboxItems(userId) {
    const items = await (await db()).getAllFromIndex("outbox", "userId", userId);
    return items.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

export async function refreshOutboxState(userId) {
    const items = userId == null ? [] : await outboxItems(userId);
    outboxState.pending = items.filter((item) => item.status === "pending").length;
    outboxState.rejected = items.filter((item) => item.status === "rejected").length;
}

/**
 * Uploads this user's pending matches, oldest first. Safe to call often: only one flush runs at a time.
 * Stops at the first network failure or expired session (the rest will go next time).
 * A match the server refuses (400) is marked "rejected" with the server's reason, and skipped
 * until the player fixes or discards it: retrying it unchanged would fail forever.
 *
 * @returns {Promise<{ sent: number, rejected: number, stoppedBy: null | "offline" | "session" }>}
 */
export function flushOutbox(userId) {
    if (!flushing) {
        flushing = doFlush(userId).finally(() => {
            flushing = null;
        });
    }
    return flushing;
}

async function doFlush(userId) {
    const result = { sent: 0, rejected: 0, stoppedBy: null };
    if (userId == null) {
        return result;
    }
    outboxState.syncing = true;
    try {
        const database = await db();
        for (const item of await outboxItems(userId)) {
            if (item.status !== "pending") {
                continue;
            }
            try {
                await api(`/api/matches/${item.id}`, { method: "PUT", body: item.body });
                await database.delete("outbox", item.id);
                result.sent++;
            } catch (error) {
                if (error instanceof NetworkError) {
                    result.stoppedBy = "offline";
                    break;
                }
                if (error instanceof ApiError && error.status === 401) {
                    result.stoppedBy = "session";
                    break;
                }
                const refused = error instanceof ApiError && error.status >= 400 && error.status < 500;
                await database.put("outbox", {
                    ...item,
                    status: refused ? "rejected" : "pending",
                    attempts: item.attempts + 1,
                    lastError: error.message,
                });
                if (refused) {
                    result.rejected++;
                }
            }
        }
    } finally {
        outboxState.syncing = false;
        await refreshOutboxState(userId);
    }
    return result;
}

/** Puts a rejected match back in the queue (after the player edited it, or to try again). */
export async function retryItem(userId, id) {
    const database = await db();
    const item = await database.get("outbox", id);
    if (item) {
        await database.put("outbox", { ...item, status: "pending", lastError: null });
    }
    await refreshOutboxState(userId);
}

/** Throws a match away for good. The UI must confirm first. */
export async function discardItem(userId, id) {
    await (await db()).delete("outbox", id);
    await refreshOutboxState(userId);
}
