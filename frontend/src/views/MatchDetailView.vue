<script setup>
// One match, game by game: score curve, the "frise" of points, post-game details.
// Players of the match can complete or fix the details (battlefields, mulligans, notes) here.
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppIcon from "../components/AppIcon.vue";
import PointTimeline from "../components/PointTimeline.vue";
import ScoreChart from "../components/ScoreChart.vue";
import { describeError } from "../lib/api.js";
import { FORMAT_LABEL, OUTCOME_LABEL, formatClock, formatDateTime, outcomeFor } from "../lib/format.js";
import { END_REASON_LABEL, POINT_LABEL, canEdit, deleteMatch, fetchMatch, saveMatch } from "../lib/matches.js";
import { cachedCards } from "../lib/reference.js";
import { session } from "../lib/session.js";

const route = useRoute();
const router = useRouter();

const match = ref(null);
const error = ref("");
const loading = ref(true);
const battlefields = ref([]);
const editing = ref(false);
const saving = ref(false);
const saveError = ref("");
const confirmDelete = ref(false);
/** gameId → seat → { battlefieldId, mulligan, notes } while editing */
const draft = reactive({});

async function load() {
    loading.value = true;
    error.value = "";
    try {
        match.value = await fetchMatch(route.params.id);
    } catch (e) {
        error.value = e.status === 404 ? "Ce match n'existe pas ou a été supprimé." : describeError(e);
    } finally {
        loading.value = false;
    }
}

onMounted(async () => {
    await load();
    battlefields.value = await cachedCards("battlefield");
});

const names = computed(() => match.value?.players.map((p) => p.name) ?? []);
const outcome = computed(() => (match.value ? outcomeFor(match.value, session.user?.id) : null));
const editable = computed(() => match.value && canEdit(match.value));

const title = computed(() => {
    const m = match.value;
    if (m.winnerSeat == null) return `Match nul ${m.gameWins[0]}–${m.gameWins[1]}`;
    return `${names.value[m.winnerSeat - 1]} gagne ${Math.max(...m.gameWins)}–${Math.min(...m.gameWins)}`;
});

function gameLine(game) {
    if (game.result === "DRAW") return "Égalité";
    const winner = names.value[game.result === "SEAT1" ? 0 : 1];
    return `${winner} gagne ${END_REASON_LABEL[game.endReason]}`;
}

const score = (game, seat) => game.players.find((p) => p.seat === seat)?.finalScore ?? 0;
const detail = (game, seat) => game.players.find((p) => p.seat === seat) ?? {};

function pointSplit(game, seat) {
    const counts = { CONQUER: 0, HOLD: 0, ABILITY: 0 };
    game.events.filter((e) => e.seat === seat).forEach((e) => counts[e.type]++);
    return counts;
}

function startEditing() {
    for (const game of match.value.games) {
        draft[game.id] = {};
        for (const seat of [1, 2]) {
            const d = detail(game, seat);
            draft[game.id][seat] = { battlefieldId: d.battlefield?.id ?? null, mulligan: d.mulliganCount ?? null, notes: d.notes ?? "" };
        }
    }
    saveError.value = "";
    editing.value = true;
}

async function save() {
    saving.value = true;
    saveError.value = "";
    try {
        match.value = await saveMatch(match.value, draft);
        editing.value = false;
    } catch (e) {
        saveError.value = describeError(e);
    } finally {
        saving.value = false;
    }
}

async function remove() {
    try {
        await deleteMatch(match.value.id);
        router.replace({ name: "matches" });
    } catch (e) {
        saveError.value = describeError(e);
        confirmDelete.value = false;
    }
}

const OUTCOME_TONE = { win: "bg-success/15 text-success", loss: "bg-danger/15 text-danger", draw: "bg-surface-2 text-muted" };
const SOURCE_LABEL = { LIVE: "compté en direct", MANUAL: "saisi à la main", IMPORTED: "importé" };
</script>

<template>
    <div class="mx-auto grid max-w-4xl gap-6 px-4 py-6 md:px-8 md:py-10">
        <RouterLink :to="{ name: 'matches' }" class="text-sm font-semibold text-muted hover:text-fg">← Matchs</RouterLink>

        <p v-if="loading" class="text-sm text-muted">Chargement…</p>
        <div v-else-if="error" class="card flex flex-wrap items-center justify-between gap-3 p-5 text-sm">
            <span class="text-muted">{{ error }}</span>
            <button class="btn" type="button" @click="load">Réessayer</button>
        </div>

        <template v-else-if="match">
            <header class="grid gap-2">
                <p class="label">
                    {{ FORMAT_LABEL[match.format] }} · {{ formatDateTime(match.playedAt) }} · {{ SOURCE_LABEL[match.source] }}
                </p>
                <div class="flex flex-wrap items-center gap-3">
                    <h1 class="font-display text-3xl font-bold tracking-wide md:text-4xl">{{ title }}</h1>
                    <span v-if="outcome" class="rounded-full px-2.5 py-0.5 text-xs font-semibold" :class="OUTCOME_TONE[outcome]">
                        {{ OUTCOME_LABEL[outcome] }}
                    </span>
                </div>
            </header>

            <section class="grid gap-3 sm:grid-cols-2" aria-label="Joueurs">
                <div v-for="player in match.players" :key="player.seat" class="card flex items-center gap-4 p-4">
                    <img
                        v-if="player.legend?.imageUrl"
                        :src="player.legend.imageUrl"
                        alt=""
                        loading="lazy"
                        class="h-20 w-[58px] shrink-0 rounded-md object-cover"
                    />
                    <div class="min-w-0">
                        <p class="flex items-center gap-2 font-semibold">
                            <i class="inline-block size-2.5 rounded-full" :class="player.seat === 1 ? 'bg-chart-me' : 'bg-chart-opp'" />
                            {{ player.name }}
                            <span v-if="!player.userId" class="text-xs font-normal text-muted">invité</span>
                        </p>
                        <p class="truncate text-sm text-muted">{{ player.legend?.name }}</p>
                        <p v-if="player.deckName" class="truncate text-xs text-muted">Deck {{ player.deckName }}</p>
                    </div>
                    <span class="ml-auto font-display text-4xl font-bold tabular-nums">{{ match.gameWins[player.seat - 1] }}</span>
                </div>
            </section>

            <section v-for="game in match.games" :key="game.id" class="card grid gap-5 p-5" :aria-labelledby="`game-${game.number}`">
                <div class="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                    <h2 :id="`game-${game.number}`" class="font-display text-2xl font-bold">
                        Game {{ game.number }} · {{ score(game, 1) }}–{{ score(game, 2) }}
                    </h2>
                    <p class="text-sm text-muted">
                        {{ gameLine(game) }} · {{ names[game.firstSeat - 1] }} commençait
                        <template v-if="game.durationSeconds != null"> · {{ formatClock(game.durationSeconds * 1000) }}</template>
                    </p>
                </div>

                <div v-if="game.events.length" class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
                    <ScoreChart :events="game.events" :names="names" />
                    <div class="grid content-start gap-4">
                        <PointTimeline :events="game.events" :names="names" :started-at="game.startedAt" />
                        <table class="w-full text-sm">
                            <caption class="sr-only">Points par type</caption>
                            <thead>
                                <tr class="text-left text-xs text-muted">
                                    <th class="py-1 font-medium">Joueur</th>
                                    <th v-for="(label, type) in POINT_LABEL" :key="type" class="py-1 text-right font-medium">
                                        <span class="inline-flex items-center gap-1.5">
                                            <i class="inline-block size-2 rounded-full" :class="`bg-chart-${type.toLowerCase()}`" />{{ label }}
                                        </span>
                                    </th>
                                </tr>
                            </thead>
                            <tbody class="tabular-nums">
                                <tr v-for="seat in [1, 2]" :key="seat" class="border-t border-line">
                                    <td class="py-1.5">{{ names[seat - 1] }}</td>
                                    <td v-for="(count, type) in pointSplit(game, seat)" :key="type" class="py-1.5 text-right">{{ count }}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
                <p v-else class="text-sm text-muted">Score saisi à la main, sans le détail des points.</p>

                <div v-if="!editing" class="grid gap-3 sm:grid-cols-2">
                    <dl v-for="seat in [1, 2]" :key="seat" class="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
                        <dt class="col-span-2 font-semibold">{{ names[seat - 1] }}</dt>
                        <dt class="text-muted">Battlefield</dt>
                        <dd>{{ detail(game, seat).battlefield?.name ?? "—" }}</dd>
                        <dt class="text-muted">Mulligan</dt>
                        <dd>{{ detail(game, seat).mulliganCount ?? "—" }}</dd>
                        <template v-if="detail(game, seat).notes">
                            <dt class="text-muted">Notes</dt>
                            <dd class="whitespace-pre-line">{{ detail(game, seat).notes }}</dd>
                        </template>
                    </dl>
                </div>
                <div v-else class="grid gap-4 sm:grid-cols-2">
                    <fieldset v-for="seat in [1, 2]" :key="seat" class="grid gap-3">
                        <legend class="mb-1 font-semibold">{{ names[seat - 1] }}</legend>
                        <label class="field">
                            <span>Battlefield</span>
                            <select v-model="draft[game.id][seat].battlefieldId" class="input">
                                <option :value="null">—</option>
                                <option v-for="card in battlefields" :key="card.id" :value="card.id">{{ card.name }}</option>
                            </select>
                        </label>
                        <label class="field">
                            <span>Mulligan</span>
                            <select v-model="draft[game.id][seat].mulligan" class="input">
                                <option :value="null">—</option>
                                <option v-for="n in [0, 1, 2]" :key="n" :value="n">{{ n }}</option>
                            </select>
                        </label>
                        <label class="field">
                            <span>Notes</span>
                            <textarea v-model="draft[game.id][seat].notes" class="input min-h-20 py-2" maxlength="2000" />
                        </label>
                    </fieldset>
                </div>
            </section>

            <p v-if="saveError" class="text-sm text-danger" role="alert">{{ saveError }}</p>

            <div v-if="editable" class="flex flex-wrap gap-2">
                <template v-if="editing">
                    <button class="btn btn-primary" type="button" :disabled="saving" @click="save">
                        <AppIcon name="check" class="size-4" />
                        {{ saving ? "Enregistrement…" : "Enregistrer" }}
                    </button>
                    <button class="btn" type="button" :disabled="saving" @click="editing = false">Annuler</button>
                </template>
                <template v-else>
                    <button class="btn" type="button" @click="startEditing">Compléter les détails</button>
                    <button v-if="!confirmDelete" class="btn" type="button" @click="confirmDelete = true">
                        <AppIcon name="trash" class="size-4" />
                        Supprimer
                    </button>
                </template>
            </div>
            <div v-if="confirmDelete" class="card grid gap-3 p-4" role="alert">
                <p class="text-sm">Supprimer ce match pour tout le monde ? Ses points disparaîtront aussi des stats.</p>
                <div class="flex flex-wrap gap-2">
                    <button class="btn !text-danger" type="button" @click="remove">Supprimer le match</button>
                    <button class="btn" type="button" @click="confirmDelete = false">Garder</button>
                </div>
            </div>
        </template>
    </div>
</template>
