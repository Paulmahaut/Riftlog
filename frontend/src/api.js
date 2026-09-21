// Coursework prototype: one shared demo account, no login screen (see README's
// "Scope" note). Every /api/** call goes through here so it always carries a
// valid JWT — logs in lazily on first call and retries once on a 401 (token
// expired) rather than surfacing that to the UI.

export let currentUser = null;

const DEMO_CREDENTIALS = {
    email: "you@example.com",
    password: "changeme123",
};

let tokenPromise = null;

async function login() {
    const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(DEMO_CREDENTIALS),
    });
    if (!response.ok) {
        throw new Error("Failed to log in as the shared demo account");
    }
    const data = await response.json();

    currentUser = {
        id: data.userId,
        email: data.email,
        displayName: data.displayName,
    };

    return data.token;
}

function getToken() {
    if (!tokenPromise) {
        tokenPromise = login();
    }
    return tokenPromise;
}

async function request(path, options, token) {
    return fetch(path, {
        ...options,
        headers: {
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...options.headers,
            Authorization: `Bearer ${token}`,
        },
    });
}

export async function apiFetch(path, options = {}) {
    const token = await getToken();
    const response = await request(path, options, token);

    if (response.status === 401) {
        tokenPromise = null;
        const freshToken = await getToken();
        return request(path, options, freshToken);
    }

    return response;
}
