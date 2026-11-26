import { api } from "./api.js";
import { clearSession, session, setSession, updateUser } from "./session.js";

export async function login(email, password) {
    const response = await api("/api/auth/login", {
        method: "POST",
        body: { email: email.trim(), password },
        auth: false,
    });
    await start(response);
}

export async function register(displayName, email, password) {
    const response = await api("/api/auth/register", {
        method: "POST",
        body: { displayName: displayName.trim(), email: email.trim(), password },
        auth: false,
    });
    await start(response);
}

async function start(response) {
    setSession(response.token, {
        id: response.userId,
        email: response.email,
        displayName: response.displayName,
        defaultFormat: "BO3",
    });
    await refreshProfile();
}

/** Reloads the profile (default format...) from the server. */
export async function refreshProfile() {
    const me = await api("/api/auth/me");
    updateUser({ id: me.id, email: me.email, displayName: me.displayName, defaultFormat: me.defaultFormat });
    return session.user;
}

export async function saveDefaultFormat(defaultFormat) {
    const me = await api("/api/users/me/settings", { method: "PUT", body: { defaultFormat } });
    updateUser({ defaultFormat: me.defaultFormat });
}

/** Local data (cached cards, unsent matches) stays on the device for the next login. */
export function logout() {
    clearSession();
}
