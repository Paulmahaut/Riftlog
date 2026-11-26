// The only place that talks to the backend. Every call goes through api().
import { markExpired, session } from "./session.js";

// Empty in dev: Vite proxies /api to localhost:8080. Set VITE_API_URL for a deployed build.
const BASE_URL = import.meta.env?.VITE_API_URL ?? "";

/** The server answered with an error status. `message` is the backend's ApiError message. */
export class ApiError extends Error {
    constructor(status, message, body) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.body = body;
    }
}

/** The server could not be reached at all (offline, server asleep, DNS...). */
export class NetworkError extends Error {
    constructor(cause) {
        super("Serveur injoignable");
        this.name = "NetworkError";
        this.cause = cause;
    }
}

/**
 * @param {string} path e.g. "/api/matches"
 * @param {{ method?: string, body?: unknown, auth?: boolean }} options
 */
export async function api(path, { method = "GET", body, auth = true } = {}) {
    const headers = {};
    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
    }
    if (auth && session.token) {
        headers.Authorization = `Bearer ${session.token}`;
    }

    let response;
    try {
        response = await fetch(BASE_URL + path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch (error) {
        throw new NetworkError(error);
    }

    const text = response.status === 204 ? "" : await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }

    if (!response.ok) {
        if (response.status === 401 && auth) {
            markExpired();
        }
        throw new ApiError(response.status, data?.message ?? response.statusText, data);
    }
    return data;
}

/** French message for an error caught around api(). */
export function describeError(error) {
    if (error instanceof NetworkError) {
        return "Serveur injoignable. Vérifie ta connexion et réessaie.";
    }
    if (error instanceof ApiError) {
        if (error.status === 401) return "Ta session a expiré. Reconnecte-toi.";
        if (error.status === 403) return "Tu n'as pas le droit de modifier ceci.";
        if (error.status >= 500) return "Le serveur a rencontré une erreur. Réessaie plus tard.";
        return error.message;
    }
    return "Une erreur inattendue est survenue.";
}
