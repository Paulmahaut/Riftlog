// Who is logged in on this device. Kept in localStorage so the app opens offline,
// even days later: an expired token only matters when we try to reach the server.
import { reactive } from "vue";

const STORAGE_KEY = "riftlog.session";

export const session = reactive({
    token: null,
    user: null, // { id, email, displayName, defaultFormat }
    /** The server refused the token: uploads wait until the player logs in again. */
    expired: false,
});

function read() {
    try {
        const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
        if (saved?.token && saved?.user) {
            session.token = saved.token;
            session.user = saved.user;
            session.expired = tokenExpiresAt(saved.token) < Date.now();
        }
    } catch {
        // No storage (private mode, tests): start logged out.
    }
}

function persist() {
    try {
        if (session.token) {
            localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: session.token, user: session.user }));
        } else {
            localStorage.removeItem(STORAGE_KEY);
        }
    } catch {
        // Storage unavailable: the session simply won't survive a reload.
    }
}

export function setSession(token, user) {
    session.token = token;
    session.user = user;
    session.expired = false;
    persist();
}

export function updateUser(fields) {
    session.user = { ...session.user, ...fields };
    persist();
}

export function clearSession() {
    session.token = null;
    session.user = null;
    session.expired = false;
    persist();
}

export function markExpired() {
    session.expired = true;
}

export function isLoggedIn() {
    return Boolean(session.token && session.user);
}

/** Expiry of a JWT in ms since epoch, or 0 when it cannot be read. */
export function tokenExpiresAt(token) {
    try {
        const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        return JSON.parse(atob(payload)).exp * 1000;
    } catch {
        return 0;
    }
}

read();
