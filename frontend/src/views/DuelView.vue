<script setup>
// The table counter. The phone lies between the two players: the top half is rotated for
// the opponent. Every change is saved to IndexedDB at once, and the finished match goes to
// the outbox, so nothing here needs the network.
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import AppIcon from "../components/AppIcon.vue";
import DuelHalf from "../components/DuelHalf.vue";
import PointIcon from "../components/PointIcon.vue";
import ScoreChart from "../components/ScoreChart.vue";
import { db } from "../lib/db.js";
import {
    FORMATS,
    addPoint,
    canChangeFormat,
    canUndo,
    clearLiveMatch,
    concede,
    currentGame,
    endGame,
    isDecided,
    loadLiveMatch,
    matchWinnerSeat,
    newMatch,
    next,
    pickFirst,
    player,
    recordMatch,
    saveLiveMatch,
    score,
    setFormat,
    undo,
    winsOf,
} from "../lib/duel.js";
import { FORMAT_LABEL, formatClock, legendShort } from "../lib/format.js";
import { outboxState } from "../lib/outbox.js";
import { cachedCards } from "../lib/reference.js";
import { session } from "../lib/session.js";
import { network, syncNow } from "../lib/sync.js";

const LABEL = { CONQUER: "Conquer", HOLD: "Hold", ABILITY: "Ability" };

const router = useRouter();
const match = ref(null);
const battlefields = ref([]);
const now = ref(Date.now());
const menuOpen = ref(false);
const confirmDelete = ref(false);
const sendStatus = ref("pending"); // pending | sent | rejected

const game = computed(() => (match.value ? currentGame(match.value) : null));
const names = computed(() => match.value?.players.map((slot) => slot.name) ?? []);
const phase = computed(() => match.value?.phase);

// ---------- Loading and saving ----------

onMounted(async () => {
    const live = await loadLiveMatch(session.user.id);
    if (!live) {
        router.replace({ name: "play" });
        return;
    }
    match.value = live;
    battlefields.value = await cachedCards("battlefield");
    if (live.phase === "match-over") refreshSendStatus();
});

let saving = Promise.resolve();
watch(
    match,
    (value) => {
        if (!value) return;
        // Chained so writes land in order even when points are tapped quickly.
        saving = saving.then(() => saveLiveMatch(value)).catch((e) => console.error("Could not save the match", e));
    },
    { deep: true },
);

// ---------- Screen: clock, wake lock ----------

const timer = setInterval(() => (now.value = Date.now()), 1000);
let wakeLock = null;

async function keepAwake() {
    try {
        if (!wakeLock && navigator.wakeLock && document.visibilityState === "visible") {
            wakeLock = await navigator.wakeLock.request("screen");
            wakeLock.addEventListener("release", () => (wakeLock = null));
        }
    } catch {
        // Not supported or refused (battery saver): the counter works anyway.
    }
}
const onVisible = () => document.visibilityState === "visible" && keepAwake();
onMounted(() => {
    keepAwake();
    document.addEventListener("visibilitychange", onVisible);
});
onBeforeUnmount(() => {
    clearInterval(timer);
    document.removeEventListener("visibilitychange", onVisible);
    wakeLock?.release();
});

const clock = computed(() => {
    const g = game.value;
    if (!g?.startedAt) return "00:00";
    return formatClock((g.endedAt ?? now.value) - g.startedAt);
});

// ---------- Actions ----------

function buzz() {
    try {
        navigator.vibrate?.(12);
    } catch {
        // ignore
    }
}

function onPoint(seat, type) {
    addPoint(match.value, seat, type);
    buzz();
}

function onPick(seat) {
    pickFirst(match.value, seat);
    keepAwake();
}

function onUndo() {
    if (undo(match.value)) buzz();
}

function endFromMenu(action) {
    menuOpen.value = false;
    if (action === "draw") endGame(match.value, "DRAW", "DRAW");
    else concede(match.value, action);
}

async function onNext() {
    next(match.value);
    if (match.value.phase === "match-over") {
        await recordMatch(match.value);
        sendStatus.value = "pending";
        syncNow().then(refreshSendStatus);
    }
}

// Post-game details edited after the match was recorded: record it again (same id, so no duplicate).
watch(
    () => (phase.value === "match-over" ? JSON.stringify(match.value.games.map((g) => g.details)) : null),
    async (details, before) => {
        if (details && before && details !== before) {
            await recordMatch(match.value);
            syncNow().then(refreshSendStatus);
        }
    },
);

async function refreshSendStatus() {
    if (!match.value) return;
    const item = await (await db()).get("outbox", match.value.id);
    sendStatus.value = !item ? "sent" : item.status;
}
watch(() => [outboxState.pending, outboxState.rejected, outboxState.syncing], refreshSendStatus);

async function rematch() {
    const old = match.value;
    const [me, opponent] = old.players.map(({ seat, ...slot }) => slot);
    // A guest created by the previous match keeps the same name: the server finds it again.
    match.value = newMatch({ userId: old.userId, format: old.format, me, opponent });
    await saveLiveMatch(match.value);
}

async function finish() {
    await clearLiveMatch();
    router.replace({ name: "home" });
}

async function deleteMatch() {
    await clearLiveMatch();
    router.replace({ name: "play" });
}

// ---------- Display helpers ----------

function resultLine(g) {
    if (g.result === "DRAW") return "Égalité";
    const winner = Number(g.result.slice(-1));
    if (g.endReason === "CONCEDE") return `${player(match.value, 3 - winner).name} abandonne`;
    return winner === 1 ? "Tu gagnes" : `${player(match.value, 2).name} gagne`;
}

const duration = (g) => formatClock((g.endedAt ?? now.value) - (g.startedAt ?? now.value));

function logRows(g) {
    const totals = { 1: 0, 2: 0 };
    return g.events
        .map((event) => {
            totals[event.seat]++;
            return { ...event, t: formatClock(event.at - g.startedAt), running: `${totals[1]}–${totals[2]}` };
        })
        .reverse();
}

function dotClass(index) {
    const g = match.value.games[index];
    if (!g) return "";
    if (g.result === "SEAT1") return "dot-me";
    if (g.result === "SEAT2") return "dot-opp";
    if (g.result === "DRAW") return "dot-draw";
    return "dot-current";
}

const matchTitle = computed(() => {
    const m = match.value;
    const winner = matchWinnerSeat(m);
    const tally = `${winsOf(m, 1)}–${winsOf(m, 2)}`;
    if (winner == null) return `Match nul ${tally}`;
    return winner === 1 ? `Tu gagnes le match ${tally}` : `${player(m, 2).name} gagne le match ${tally}`;
});

const totalDuration = computed(() =>
    formatClock(match.value.games.reduce((sum, g) => sum + ((g.endedAt ?? 0) - (g.startedAt ?? 0)), 0)),
);

const battlefieldName = (id) => battlefields.value.find((card) => card.id === id)?.name;
</script>

<template>
    <div v-if="match" data-theme="table" class="duel">
        <DuelHalf
            :seat="2"
            :slot="match.players[1]"
            :game="game"
            :phase="phase"
            flipped
            @point="(type) => onPoint(2, type)"
            @pick="onPick(2)"
        />

        <div class="bar">
            <button class="icon-btn" type="button" aria-label="Annuler le dernier point" :disabled="!canUndo(match)" @click="onUndo">
                <svg viewBox="0 0 24 24" class="size-[22px]" aria-hidden="true">
                    <path d="M9 14L4 9l5-5M4 9h10.5a5.5 5.5 0 010 11H11" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
            </button>
            <div class="grid justify-items-center gap-1">
                <div class="clock" role="timer" aria-label="Durée de la game">{{ clock }}</div>
                <div class="flex items-center gap-2 text-[11px] uppercase tracking-[0.08em] text-muted">
                    <span>{{ FORMAT_LABEL[match.format] }}</span>
                    <span class="flex gap-[5px]" aria-hidden="true">
                        <span v-for="i in FORMATS[match.format].maxGames" :key="i" class="dot" :class="dotClass(i - 1)" />
                    </span>
                    <span>Game {{ game.number }}</span>
                </div>
            </div>
            <button class="icon-btn" type="button" aria-label="Menu du match" @click="menuOpen = true">
                <svg viewBox="0 0 24 24" class="size-[22px]" aria-hidden="true">
                    <circle cx="5" cy="12" r="2" fill="currentColor" /><circle cx="12" cy="12" r="2" fill="currentColor" /><circle cx="19" cy="12" r="2" fill="currentColor" />
                </svg>
            </button>
        </div>

        <DuelHalf
            :seat="1"
            :slot="match.players[0]"
            :game="game"
            :phase="phase"
            @point="(type) => onPoint(1, type)"
            @pick="onPick(1)"
        />

        <!-- ⋯ menu -->
        <template v-if="menuOpen">
            <div class="scrim" @click="menuOpen = false" />
            <div class="sheet" role="dialog" aria-modal="true" aria-labelledby="menu-title">
                <div>
                    <p class="label">Match</p>
                    <h2 id="menu-title" class="sheet-title">Options</h2>
                </div>
                <div class="grid gap-2">
                    <span class="label">Format</span>
                    <div class="inline-flex w-fit overflow-hidden rounded-[10px] border border-line" role="group" aria-label="Format">
                        <button
                            v-for="format in ['BO1', 'BO3']"
                            :key="format"
                            type="button"
                            class="px-[18px] py-2.5 font-semibold disabled:opacity-40"
                            :class="match.format === format ? 'bg-fg text-bg' : 'text-muted'"
                            :aria-pressed="match.format === format"
                            :disabled="!canChangeFormat(match)"
                            @click="setFormat(match, format)"
                        >
                            {{ FORMAT_LABEL[format] }}
                        </button>
                    </div>
                    <p v-if="!canChangeFormat(match)" class="text-xs text-muted">Le format ne change plus une fois le premier point marqué.</p>
                </div>
                <div class="grid gap-2">
                    <span class="label">Fin de game</span>
                    <div class="sheet-row">
                        <button class="btn" type="button" :disabled="phase !== 'playing'" @click="endFromMenu('draw')">Égalité</button>
                        <button class="btn !text-danger" type="button" :disabled="phase !== 'playing'" @click="endFromMenu(1)">
                            {{ names[0] }} abandonne
                        </button>
                        <button class="btn !text-danger" type="button" :disabled="phase !== 'playing'" @click="endFromMenu(2)">
                            {{ names[1] }} abandonne
                        </button>
                    </div>
                </div>
                <div v-if="confirmDelete" class="grid gap-3 rounded-xl border border-line p-4" role="alert">
                    <p class="text-sm">Le match sera supprimé sans être enregistré.</p>
                    <div class="sheet-row">
                        <button class="btn !text-danger" type="button" @click="deleteMatch">Supprimer</button>
                        <button class="btn" type="button" @click="confirmDelete = false">Garder</button>
                    </div>
                </div>
                <div class="sheet-row">
                    <RouterLink :to="{ name: 'play' }" class="btn">Quitter (reprendre plus tard)</RouterLink>
                    <button v-if="!confirmDelete" class="btn" type="button" @click="confirmDelete = true">Supprimer le match</button>
                </div>
                <button class="btn btn-primary" type="button" @click="menuOpen = false">Reprendre</button>
            </div>
        </template>

        <!-- End of a game -->
        <template v-if="phase === 'game-over'">
            <div class="scrim" />
            <div class="sheet" role="dialog" aria-modal="true" aria-labelledby="game-title">
                <div>
                    <p class="label">Game {{ game.number }} terminée</p>
                    <h2 id="game-title" class="sheet-title">{{ resultLine(game) }} · {{ score(game, 1) }}–{{ score(game, 2) }}</h2>
                    <p class="mt-1 text-[13px] text-muted">Durée {{ duration(game) }} · {{ names[game.firstSeat - 1] }} commençait</p>
                </div>
                <ScoreChart :events="game.events" :names="names" />
                <div class="log">
                    <p v-if="game.events.length === 0" class="p-1.5 text-xs text-muted">Aucun point marqué.</p>
                    <div v-for="row in logRows(game)" :key="row.id" class="log-row">
                        <span class="tick" :style="{ '--c': `var(--${row.type.toLowerCase()})` }"><PointIcon :type="row.type" class="size-3" /></span>
                        <span>
                            <b class="font-semibold" :class="row.seat === 1 ? 'text-seat-me' : 'text-seat-opp'">{{ names[row.seat - 1] }}</b>
                            · {{ LABEL[row.type] }}
                        </span>
                        <span class="text-xs text-muted">{{ row.t }}</span>
                        <span class="font-display text-base font-semibold">{{ row.running }}</span>
                    </div>
                </div>
                <details class="rounded-xl border border-line px-3.5 py-3">
                    <summary class="cursor-pointer text-sm font-semibold">Compléter (facultatif)</summary>
                    <div class="mt-3.5 grid grid-cols-2 gap-3">
                        <label v-for="seat in [1, 2]" :key="`bf${seat}`" class="field min-w-0">
                            <span>Battlefield · {{ names[seat - 1] }}</span>
                            <select v-model="game.details[seat].battlefieldId" class="input">
                                <option :value="null">—</option>
                                <option v-for="card in battlefields" :key="card.id" :value="card.id">{{ card.name }}</option>
                            </select>
                        </label>
                        <label v-for="seat in [1, 2]" :key="`mu${seat}`" class="field min-w-0">
                            <span>Mulligan · {{ names[seat - 1] }}</span>
                            <select v-model="game.details[seat].mulligan" class="input">
                                <option :value="null">—</option>
                                <option v-for="n in [0, 1, 2]" :key="n" :value="n">{{ n }}</option>
                            </select>
                        </label>
                    </div>
                    <p class="mt-3 text-xs text-muted">Le side et les autres détails se rempliront plus tard depuis le PC.</p>
                </details>
                <div class="sheet-row">
                    <button class="btn" type="button" @click="onUndo">
                        {{ game.endReason === "POINTS" ? "Annuler le dernier point" : "Rouvrir la game" }}
                    </button>
                    <button class="btn btn-primary" type="button" @click="onNext">
                        {{ isDecided(match) ? "Voir le match" : `Game ${game.number + 1}` }}
                    </button>
                </div>
            </div>
        </template>

        <!-- End of the match -->
        <template v-if="phase === 'match-over'">
            <div class="scrim" />
            <div class="sheet" role="dialog" aria-modal="true" aria-labelledby="match-title">
                <div>
                    <p class="label">
                        {{ FORMAT_LABEL[match.format] }} · {{ legendShort(match.players[0].legendName) }} contre
                        {{ legendShort(match.players[1].legendName) }}
                    </p>
                    <h2 id="match-title" class="sheet-title">{{ matchTitle }}</h2>
                    <p class="mt-1 text-[13px] text-muted">
                        {{ match.games.length }} game{{ match.games.length > 1 ? "s" : "" }} · {{ totalDuration }} au total
                    </p>
                </div>
                <div class="grid gap-2">
                    <div v-for="g in match.games" :key="g.id" class="game-row">
                        <span class="font-display font-semibold tracking-[0.08em] text-muted">G{{ g.number }}</span>
                        <div>
                            <div>{{ resultLine(g) }}</div>
                            <div class="text-xs text-muted">
                                {{ names[g.firstSeat - 1] }} commençait · {{ duration(g) }}
                                <template v-for="seat in [1, 2]" :key="seat">
                                    <template v-if="battlefieldName(g.details[seat].battlefieldId)"> · {{ battlefieldName(g.details[seat].battlefieldId) }}</template>
                                </template>
                            </div>
                        </div>
                        <span class="font-display text-[22px] font-bold tabular-nums">{{ score(g, 1) }}–{{ score(g, 2) }}</span>
                    </div>
                </div>
                <p class="status" :class="`status-${sendStatus}`" role="status">
                    <AppIcon :name="sendStatus === 'sent' ? 'check' : sendStatus === 'rejected' ? 'alert' : 'upload'" class="size-4" />
                    <template v-if="sendStatus === 'sent'">Enregistré et envoyé</template>
                    <template v-else-if="sendStatus === 'rejected'">Refusé par le serveur · voir Réglages</template>
                    <template v-else-if="!network.online">Enregistré sur le téléphone · partira au retour du réseau</template>
                    <template v-else>Enregistré sur le téléphone · envoi en cours</template>
                </p>
                <div class="sheet-row">
                    <button class="btn" type="button" @click="rematch">Revanche</button>
                    <button class="btn btn-primary" type="button" @click="finish">Terminer</button>
                </div>
            </div>
        </template>
    </div>
</template>

<style scoped>
.duel {
    height: 100dvh;
    max-width: 560px;
    margin: 0 auto;
    display: grid;
    grid-template-rows: 1fr auto 1fr;
    overflow: hidden;
    background: var(--bg);
    color: var(--fg);
    user-select: none;
    -webkit-user-select: none;
}
.bar {
    display: grid;
    grid-template-columns: 52px 1fr 52px;
    align-items: center;
    height: 58px;
    background: color-mix(in srgb, black 35%, var(--bg));
    border-block: 1px solid var(--line);
    padding-inline: max(8px, env(safe-area-inset-left, 0px)) max(8px, env(safe-area-inset-right, 0px));
}
.icon-btn {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: grid;
    place-items: center;
    color: var(--muted);
}
.icon-btn:active {
    background: var(--surface);
}
.icon-btn:disabled {
    opacity: 0.3;
}
.clock {
    font-family: var(--font-display);
    font-weight: 600;
    font-size: 26px;
    line-height: 1;
    font-variant-numeric: tabular-nums;
    letter-spacing: 0.04em;
}
.dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    border: 1.5px solid var(--line);
}
.dot-current {
    border-color: var(--fg);
}
.dot-me {
    background: var(--seat-me);
    border-color: var(--seat-me);
}
.dot-opp {
    background: var(--seat-opp);
    border-color: var(--seat-opp);
}
.dot-draw {
    background: var(--muted);
    border-color: var(--muted);
}

.scrim {
    position: fixed;
    inset: 0;
    z-index: 10;
    background: color-mix(in srgb, black 60%, transparent);
}
.sheet {
    position: fixed;
    z-index: 11;
    inset-inline: 0;
    bottom: 0;
    max-width: 560px;
    max-height: 88%;
    margin: 0 auto;
    overflow-y: auto;
    overscroll-behavior: contain;
    display: grid;
    gap: 16px;
    padding-block: 20px calc(20px + env(safe-area-inset-bottom, 0px));
    padding-inline: max(18px, env(safe-area-inset-left, 0px)) max(18px, env(safe-area-inset-right, 0px));
    background: var(--surface);
    border-top: 1px solid var(--line);
    border-radius: 18px 18px 0 0;
    animation: up 0.22s ease-out;
    user-select: text;
    -webkit-user-select: text;
}
@keyframes up {
    from {
        transform: translateY(24px);
        opacity: 0;
    }
}
.sheet-title {
    margin: 0;
    font-family: var(--font-display);
    font-weight: 700;
    font-size: 30px;
    line-height: 1.05;
    text-wrap: balance;
}
.sheet-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}
.sheet-row > * {
    flex: 1 1 140px;
    min-height: 48px;
}
.log {
    display: grid;
    gap: 2px;
    max-height: 190px;
    overflow-y: auto;
    border: 1px solid var(--line);
    border-radius: 12px;
    padding: 6px;
}
.log-row {
    display: grid;
    grid-template-columns: 20px 1fr auto auto;
    gap: 10px;
    align-items: center;
    padding: 5px 6px;
    font-size: 13px;
    font-variant-numeric: tabular-nums;
}
.tick {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: grid;
    place-items: center;
    background: var(--c);
    color: var(--bg);
}
.game-row {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 12px;
    align-items: center;
    padding: 12px 14px;
    border: 1px solid var(--line);
    border-radius: 12px;
    font-size: 14px;
}
.status {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    justify-self: start;
    margin: 0;
    padding: 6px 12px;
    border-radius: 999px;
    border: 1px dashed currentColor;
    font-size: 12px;
}
.status-pending {
    color: var(--warning);
}
.status-sent {
    color: var(--success);
    border-style: solid;
}
.status-rejected {
    color: var(--danger);
}
</style>
