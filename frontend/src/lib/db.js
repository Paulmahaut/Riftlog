// Local database (IndexedDB) so the phone works without network:
//   cards   — legends and battlefields, for the pickers
//   players — registered players and guests, for choosing an opponent
//   outbox  — finished matches waiting to be uploaded
//   meta    — small key/value records (last sync dates...)
import { openDB } from "idb";

const DB_NAME = "riftlog";
const DB_VERSION = 1;

let dbPromise = null;

export function db() {
    if (!dbPromise) {
        dbPromise = openDB(DB_NAME, DB_VERSION, {
            upgrade(database) {
                const cards = database.createObjectStore("cards", { keyPath: "id" });
                cards.createIndex("type", "type");
                database.createObjectStore("players", { keyPath: "key" });
                const outbox = database.createObjectStore("outbox", { keyPath: "id" });
                outbox.createIndex("userId", "userId");
                database.createObjectStore("meta", { keyPath: "key" });
            },
        });
    }
    return dbPromise;
}

/** Tests only: forget the open connection so the next call starts from a fresh database. */
export async function closeDbForTests() {
    if (dbPromise) {
        (await dbPromise).close();
        dbPromise = null;
    }
}
