<script setup>
// Starting a match: format, legends, opponent. Everything comes from the device cache,
// so it works at the table without network. A match in progress can be resumed from here.
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import AppIcon from "../components/AppIcon.vue";
import { clearLiveMatch, currentGame, loadLiveMatch, newMatch, saveLiveMatch, score } from "../lib/duel.js";
import { FORMAT_LABEL } from "../lib/format.js";
import { cachedCards, cachedPlayers, referenceState } from "../lib/reference.js";
import { session } from "../lib/session.js";
import { network, syncNow } from "../lib/sync.js";

const LAST_SETUP_KEY = "riftlog.lastSetup";
const NEW_GUEST = "new-guest";

const router = useRouter();

const loading = ref(true);
const legends = ref([]);
const players = ref([]);
const live = ref(null);
const confirmAbandon = ref(false);
const error = ref("");

const form = reactive({
    format: session.user?.defaultFormat ?? "BO3",
    legendId: "",
    opponentKey: "",
    guestName: "",
    opponentLegendId: "",
});

const friends = computed(() => players.value.filter((p) => p.kind === "user" && p.id !== session.user.id));
const guests = computed(() => players.value.filter((p) => p.kind === "guest"));

// First login: the legends are still downloading when this screen opens. Reload once they are in.
watch(
    () => referenceState.version,
    async () => {
        legends.value = await cachedCards("legend");
        players.value = await cachedPlayers();
    },
);

onMounted(async () => {
    const [cards, cachedList, match] = await Promise.all([
        cachedCards("legend"),
        cachedPlayers(),
        loadLiveMatch(session.user.id),
    ]);
    legends.value = cards;
    players.value = cachedList;
    if (match?.phase === "match-over") {
        // Already in the outbox: nothing left to resume.
        await clearLiveMatch();
    } else {
        live.value = match;
    }

    const last = readLastSetup();
    const known = (id) => cards.some((card) => card.id === id);
    if (known(last.legendId)) form.legendId = last.legendId;
    if (last.opponentKey && cachedList.some((p) => p.key === last.opponentKey)) form.opponentKey = last.opponentKey;
    if (known(last.opponentLegendId)) form.opponentLegendId = last.opponentLegendId;
    loading.value = false;
});

function readLastSetup() {
    try {
        return JSON.parse(localStorage.getItem(LAST_SETUP_KEY)) ?? {};
    } catch {
        return {};
    }
}

const liveSummary = computed(() => {
    const match = live.value;
    if (!match) return "";
    const game = currentGame(match);
    return `${FORMAT_LABEL[match.format]} · Game ${game.number} · ${score(game, 1)}–${score(game, 2)}`;
});

const canStart = computed(
    () =>
        form.legendId &&
        form.opponentLegendId &&
        (form.opponentKey === NEW_GUEST ? form.guestName.trim().length > 0 : form.opponentKey),
);

function opponentSlot() {
    const legend = legends.value.find((card) => card.id === form.opponentLegendId);
    const base = { legendId: legend.id, legendName: legend.name };
    if (form.opponentKey === NEW_GUEST) {
        const name = form.guestName.trim();
        // A guest already known under this name is reused instead of creating a duplicate.
        const existing = guests.value.find((g) => g.name.toLowerCase() === name.toLowerCase());
        return existing
            ? { ...base, kind: "guest", id: existing.id, name: existing.name }
            : { ...base, kind: "new-guest", id: null, name };
    }
    const picked = players.value.find((p) => p.key === form.opponentKey);
    return { ...base, kind: picked.kind, id: picked.id, name: picked.name };
}

async function start() {
    error.value = "";
    if (!canStart.value) {
        error.value = "Choisis les deux légendes et ton adversaire.";
        return;
    }
    const legend = legends.value.find((card) => card.id === form.legendId);
    const match = newMatch({
        userId: session.user.id,
        format: form.format,
        me: { kind: "user", id: session.user.id, name: session.user.displayName, legendId: legend.id, legendName: legend.name },
        opponent: opponentSlot(),
    });
    localStorage.setItem(
        LAST_SETUP_KEY,
        JSON.stringify({
            legendId: form.legendId,
            opponentKey: form.opponentKey === NEW_GUEST ? "" : form.opponentKey,
            opponentLegendId: form.opponentLegendId,
        }),
    );
    await saveLiveMatch(match);
    router.push({ name: "duel" });
}

async function abandon() {
    await clearLiveMatch();
    live.value = null;
    confirmAbandon.value = false;
}
</script>

<template>
    <div class="mx-auto grid max-w-xl gap-5 px-4 py-6 md:px-8 md:py-10">
        <h1 class="font-display text-3xl font-bold tracking-wide">Jouer</h1>

        <p v-if="loading" class="text-sm text-muted">Chargement…</p>

        <section v-else-if="live" class="card grid gap-4 p-5" aria-labelledby="live-title">
            <div>
                <p class="label">Match en cours</p>
                <p id="live-title" class="mt-1 font-display text-2xl font-bold">
                    {{ live.players[0].name }} contre {{ live.players[1].name }}
                </p>
                <p class="mt-1 text-sm text-muted">{{ liveSummary }}</p>
            </div>
            <div class="flex flex-wrap gap-2">
                <RouterLink :to="{ name: 'duel' }" class="btn btn-primary flex-1">
                    <AppIcon name="play" class="size-4" />
                    Reprendre
                </RouterLink>
                <button v-if="!confirmAbandon" type="button" class="btn" @click="confirmAbandon = true">Abandonner</button>
            </div>
            <div v-if="confirmAbandon" class="grid gap-3 rounded-xl border border-line p-4" role="alert">
                <p class="text-sm">Ce match sera supprimé sans être enregistré. Les points déjà comptés seront perdus.</p>
                <div class="flex flex-wrap gap-2">
                    <button type="button" class="btn flex-1 !text-danger" @click="abandon">Supprimer le match</button>
                    <button type="button" class="btn flex-1" @click="confirmAbandon = false">Garder</button>
                </div>
            </div>
        </section>

        <div v-else-if="legends.length === 0 && referenceState.loading" class="card grid gap-2 p-5" role="status">
            <p class="font-semibold">Téléchargement des légendes…</p>
            <p class="text-sm text-muted">Quelques secondes, une seule fois : ensuite le compteur marche sans réseau.</p>
        </div>

        <div v-else-if="legends.length === 0" class="card grid gap-3 p-5">
            <p class="font-semibold">Les légendes ne sont pas encore sur cet appareil.</p>
            <p class="text-sm text-muted">
                Il faut du réseau une seule fois pour les télécharger. Ensuite, le compteur marche sans réseau.
            </p>
            <button v-if="network.online" class="btn w-fit" type="button" @click="syncNow({ forceReference: true })">
                Télécharger maintenant
            </button>
        </div>

        <form v-else class="card grid gap-5 p-5" @submit.prevent="start">
            <div class="grid gap-2">
                <span class="label">Format</span>
                <div class="inline-flex w-fit overflow-hidden rounded-xl border border-line" role="group" aria-label="Format">
                    <button
                        v-for="format in ['BO1', 'BO3']"
                        :key="format"
                        type="button"
                        class="px-5 py-2.5 font-semibold"
                        :class="form.format === format ? 'bg-fg text-bg' : 'text-muted'"
                        :aria-pressed="form.format === format"
                        @click="form.format = format"
                    >
                        {{ FORMAT_LABEL[format] }}
                    </button>
                </div>
            </div>

            <label class="field">
                <span>Ma légende</span>
                <select v-model="form.legendId" class="input" required>
                    <option value="" disabled>Choisir…</option>
                    <option v-for="legend in legends" :key="legend.id" :value="legend.id">{{ legend.name }}</option>
                </select>
            </label>

            <label class="field">
                <span>Adversaire</span>
                <select v-model="form.opponentKey" class="input" required>
                    <option value="" disabled>Choisir…</option>
                    <optgroup v-if="friends.length" label="Amis">
                        <option v-for="p in friends" :key="p.key" :value="p.key">{{ p.name }}</option>
                    </optgroup>
                    <optgroup v-if="guests.length" label="Invités">
                        <option v-for="p in guests" :key="p.key" :value="p.key">{{ p.name }}</option>
                    </optgroup>
                    <option :value="NEW_GUEST">+ Nouvel invité…</option>
                </select>
            </label>

            <label v-if="form.opponentKey === NEW_GUEST" class="field">
                <span>Nom de l'invité</span>
                <input v-model="form.guestName" class="input" maxlength="100" autocomplete="off" placeholder="Thomas" />
            </label>

            <label class="field">
                <span>Légende adverse</span>
                <select v-model="form.opponentLegendId" class="input" required>
                    <option value="" disabled>Choisir…</option>
                    <option v-for="legend in legends" :key="legend.id" :value="legend.id">{{ legend.name }}</option>
                </select>
            </label>

            <p v-if="error" class="text-sm text-danger" role="alert">{{ error }}</p>

            <button type="submit" class="btn btn-primary min-h-12 text-base" :disabled="!canStart">
                <AppIcon name="play" class="size-4" />
                Lancer le compteur
            </button>
            <p class="text-xs text-muted">
                Qui commence se choisit sur l'écran de duel. Battlefields et mulligans se notent après chaque game.
            </p>
        </form>

        <p v-if="!loading && !live && friends.length === 0 && legends.length" class="text-xs text-muted">
            Tes amis apparaîtront ici dès qu'ils auront créé leur compte.
        </p>
    </div>
</template>
