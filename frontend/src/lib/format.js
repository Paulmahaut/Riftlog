const dateFormatter = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short" });
const dateYearFormatter = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short", year: "numeric" });
const timeFormatter = new Intl.DateTimeFormat("fr-FR", { hour: "2-digit", minute: "2-digit" });

/** "26 nov. · 18:05", with the year when it is not the current one. */
export function formatDateTime(iso) {
    const date = new Date(iso);
    const day = date.getFullYear() === new Date().getFullYear() ? dateFormatter.format(date) : dateYearFormatter.format(date);
    return `${day} · ${timeFormatter.format(date)}`;
}

/** "il y a 5 min", "il y a 3 h", or a date. */
export function formatRelative(iso) {
    const minutes = Math.round((Date.now() - Date.parse(iso)) / 60000);
    if (minutes < 1) return "à l'instant";
    if (minutes < 60) return `il y a ${minutes} min`;
    if (minutes < 24 * 60) return `il y a ${Math.round(minutes / 60)} h`;
    return formatDateTime(iso);
}

export const FORMAT_LABEL = { BO1: "Bo1", BO3: "Bo3" };

/** "win" | "loss" | "draw" from the point of view of `userId`, or null if they did not play. */
export function outcomeFor(match, userId) {
    const me = match.players.find((player) => player.userId === userId);
    if (!me) return null;
    if (match.winnerSeat == null) return "draw";
    return match.winnerSeat === me.seat ? "win" : "loss";
}

export const OUTCOME_LABEL = { win: "Victoire", loss: "Défaite", draw: "Nul" };
