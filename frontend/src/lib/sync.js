// Background synchronisation: uploads waiting matches and refreshes cached data
// whenever the device is (back) online. Nothing here blocks the UI.
import { reactive } from "vue";
import { NetworkError } from "./api.js";
import { refreshProfile } from "./auth.js";
import { flushOutbox, refreshOutboxState } from "./outbox.js";
import { refreshReferenceData, referenceStatus } from "./reference.js";
import { isLoggedIn, session } from "./session.js";

const REFERENCE_MAX_AGE_MS = 24 * 60 * 60 * 1000;

export const network = reactive({
    online: typeof navigator === "undefined" ? true : navigator.onLine,
});

/** Runs everything that needs the server. Errors are swallowed: the next trigger retries. */
export async function syncNow({ forceReference = false } = {}) {
    if (!isLoggedIn()) {
        return;
    }
    const userId = session.user.id;
    await refreshOutboxState(userId);
    if (session.expired) {
        return;
    }
    try {
        const { sent } = await flushOutbox(userId);
        const status = await referenceStatus();
        const stale = !status || Date.now() - Date.parse(status.syncedAt) > REFERENCE_MAX_AGE_MS;
        // After an upload too: a guest added offline now exists on the server and gets an id.
        if (forceReference || stale || sent > 0) {
            await refreshReferenceData();
            await refreshProfile();
        }
    } catch (error) {
        if (!(error instanceof NetworkError)) {
            console.warn("Sync failed", error);
        }
    }
}

export function startAutoSync() {
    window.addEventListener("online", () => {
        network.online = true;
        syncNow();
    });
    window.addEventListener("offline", () => {
        network.online = false;
    });
    document.addEventListener("visibilitychange", () => {
        if (document.visibilityState === "visible") {
            syncNow();
        }
    });
    syncNow();
}
