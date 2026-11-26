// Reference data cached on the device: legends, battlefields, players and guests.
// Refreshed when online; read from IndexedDB at the table.
import { reactive } from "vue";
import { api } from "./api.js";
import { db } from "./db.js";

/** `version` goes up after each refresh, so screens can reload what they read from the cache. */
export const referenceState = reactive({ loading: false, version: 0 });

const CACHED_CARD_TYPES = ["legend", "battlefield"];

export async function refreshReferenceData() {
    referenceState.loading = true;
    try {
        return await downloadReferenceData();
    } finally {
        referenceState.loading = false;
    }
}

async function downloadReferenceData() {
    const [legends, battlefields, users, guests] = await Promise.all([
        api("/api/cards?type=legend"),
        api("/api/cards?type=battlefield"),
        api("/api/users"),
        api("/api/guests"),
    ]);

    const database = await db();
    const tx = database.transaction(["cards", "players", "meta"], "readwrite");
    await tx.objectStore("cards").clear();
    for (const card of [...legends, ...battlefields]) {
        await tx.objectStore("cards").put(card);
    }
    await tx.objectStore("players").clear();
    for (const user of users) {
        await tx.objectStore("players").put({ key: `user:${user.id}`, kind: "user", id: user.id, name: user.displayName });
    }
    for (const guest of guests) {
        await tx.objectStore("players").put({ key: `guest:${guest.id}`, kind: "guest", id: guest.id, name: guest.name });
    }
    const status = {
        key: "reference",
        syncedAt: new Date().toISOString(),
        legends: legends.length,
        battlefields: battlefields.length,
        players: users.length + guests.length,
    };
    await tx.objectStore("meta").put(status);
    await tx.done;
    referenceState.version++;
    return status;
}

/** @param {"legend"|"battlefield"} type */
export async function cachedCards(type) {
    if (!CACHED_CARD_TYPES.includes(type)) {
        throw new Error(`Only ${CACHED_CARD_TYPES.join(", ")} cards are cached`);
    }
    const cards = await (await db()).getAllFromIndex("cards", "type", type.toUpperCase());
    return cards.sort((a, b) => a.name.localeCompare(b.name));
}

export async function cachedPlayers() {
    const players = await (await db()).getAll("players");
    return players.sort((a, b) => a.name.localeCompare(b.name));
}

/** { syncedAt, legends, battlefields, players } or undefined if never synced on this device. */
export async function referenceStatus() {
    return (await db()).get("meta", "reference");
}
