<script setup>
// A player's statistics (SPEC §3). Everyone in the group can look at everyone.
// Computed by the server from the stored points: GET /api/stats?userId=
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import PointSplitBar from "../components/PointSplitBar.vue";
import WinBar from "../components/WinBar.vue";
import { api, describeError, NetworkError } from "../lib/api.js";
import { formatClock, formatDateTime } from "../lib/format.js";
import { winRate } from "../lib/matches.js";
import { cachedPlayers } from "../lib/reference.js";
import { session } from "../lib/session.js";
import { network } from "../lib/sync.js";

const route = useRoute();
const router = useRouter();

const players = ref([]);
const stats = ref(null);
const loading = ref(true);
const error = ref("");
const offline = ref(false);
const battlefieldSide = ref("mine");

const playerId = computed(() => Number(route.query.player) || session.user.id);
const isMe = computed(() => playerId.value === session.user.id);

async function load() {
    loading.value = true;
    error.value = "";
    offline.value = false;
    try {
        stats.value = await api(`/api/stats?userId=${playerId.value}`);
    } catch (e) {
        offline.value = e instanceof NetworkError && !network.online;
        error.value = describeError(e);
    } finally {
        loading.value = false;
    }
}

onMounted(async () => {
    players.value = (await cachedPlayers()).filter((p) => p.kind === "user");
    await load();
});
watch(playerId, load);
watch(
    () => network.online,
    (online) => online && error.value && load(),
);

function choosePlayer(event) {
    const id = Number(event.target.value);
    router.replace({ query: id === session.user.id ? {} : { player: id } });
}

const tiles = computed(() => {
    const s = stats.value;
    return [
        { label: "Matchs", tally: s.matches },
        { label: "Games", tally: s.games },
        { label: "En commençant", tally: s.goingFirst },
        { label: "En second", tally: s.goingSecond },
    ];
});

const record = (t) => `${t.wins} V · ${t.losses} D${t.draws ? ` · ${t.draws} N` : ""}`;
const battlefieldLines = computed(() =>
    battlefieldSide.value === "mine" ? stats.value.myBattlefields : stats.value.opponentBattlefields,
);
</script>

<template>
    <div class="mx-auto grid max-w-5xl gap-6 px-4 py-6 md:px-8 md:py-10">
        <div class="flex flex-wrap items-center justify-between gap-3">
            <h1 class="font-display text-3xl font-bold tracking-wide">Stats</h1>
            <label class="flex items-center gap-2 text-sm">
                <span class="text-muted">Joueur</span>
                <select class="input w-auto min-w-44" :value="playerId" @change="choosePlayer">
                    <option v-for="p in players" :key="p.id" :value="p.id">{{ p.id === session.user.id ? `${p.name} (moi)` : p.name }}</option>
                </select>
            </label>
        </div>

        <p v-if="loading" class="text-sm text-muted">Chargement…</p>
        <p v-else-if="offline" class="card p-5 text-sm text-muted">Les stats s'afficheront au retour du réseau.</p>
        <div v-else-if="error" class="card flex flex-wrap items-center justify-between gap-3 p-5 text-sm">
            <span class="text-muted">{{ error }}</span>
            <button class="btn" type="button" @click="load">Réessayer</button>
        </div>

        <p v-else-if="stats.games.played === 0" class="card p-5 text-sm text-muted">
            {{ isMe ? "Tu n'as" : `${stats.player.displayName} n'a` }} encore joué aucune game. Les stats apparaîtront
            après le premier match.
        </p>

        <template v-else>
            <!-- Headline numbers -->
            <section class="grid grid-cols-2 gap-3 lg:grid-cols-5" aria-label="Résumé">
                <div v-for="tile in tiles" :key="tile.label" class="card grid gap-1 p-4">
                    <p class="label">{{ tile.label }}</p>
                    <p class="font-display text-4xl font-bold tabular-nums">
                        {{ winRate(tile.tally) == null ? "—" : `${winRate(tile.tally)} %` }}
                    </p>
                    <p class="text-xs text-muted tabular-nums">{{ tile.tally.played }} joué{{ tile.tally.played > 1 ? "s" : "" }} · {{ record(tile.tally) }}</p>
                </div>
                <div class="card col-span-2 grid gap-1 p-4 lg:col-span-1">
                    <p class="label">Durée moyenne</p>
                    <p class="font-display text-4xl font-bold tabular-nums">
                        {{ stats.averageGameSeconds == null ? "—" : formatClock(stats.averageGameSeconds * 1000) }}
                    </p>
                    <p class="text-xs text-muted">par game chronométrée</p>
                </div>
            </section>

            <!-- 1. Against each legend, going first or not -->
            <section class="card grid gap-3 p-5" aria-labelledby="vs-legends">
                <div>
                    <h2 id="vs-legends" class="font-display text-2xl font-bold">Contre chaque légende</h2>
                    <p class="text-sm text-muted">Pourcentage de games gagnées, selon qui commence.</p>
                </div>
                <div class="overflow-x-auto">
                    <table class="w-full min-w-[560px] text-sm">
                        <thead>
                            <tr class="text-left text-xs text-muted">
                                <th class="py-2 pr-3 font-medium">Légende adverse</th>
                                <th class="py-2 pr-3 text-right font-medium">Games</th>
                                <th class="py-2 pr-3 font-medium">Toutes</th>
                                <th class="py-2 pr-3 font-medium">En commençant</th>
                                <th class="py-2 font-medium">En second</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="line in stats.opponentLegends" :key="line.legend.id" class="border-t border-line">
                                <td class="py-2 pr-3">{{ line.legend.name }}</td>
                                <td class="py-2 pr-3 text-right tabular-nums">{{ line.all.played }}</td>
                                <td class="py-2 pr-3"><WinBar :tally="line.all" /></td>
                                <td class="py-2 pr-3"><WinBar :tally="line.goingFirst" /></td>
                                <td class="py-2"><WinBar :tally="line.goingSecond" /></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            <div class="grid gap-6 lg:grid-cols-2">
                <!-- 2. Point split -->
                <section class="card grid content-start gap-5 p-5" aria-labelledby="points">
                    <div>
                        <h2 id="points" class="font-display text-2xl font-bold">Répartition des points</h2>
                        <p class="text-sm text-muted">Sur les {{ stats.pointsScored.games }} games comptées point par point.</p>
                    </div>
                    <PointSplitBar label="Marqués" :split="stats.pointsScored" />
                    <PointSplitBar label="Encaissés" :split="stats.pointsConceded" />
                </section>

                <!-- Own legends -->
                <section class="card grid content-start gap-3 p-5" aria-labelledby="my-legends">
                    <h2 id="my-legends" class="font-display text-2xl font-bold">Avec mes légendes</h2>
                    <table class="w-full text-sm">
                        <thead>
                            <tr class="text-left text-xs text-muted">
                                <th class="py-2 pr-3 font-medium">Légende</th>
                                <th class="py-2 pr-3 text-right font-medium">Games</th>
                                <th class="py-2 font-medium">Victoires</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="line in stats.myLegends" :key="line.legend.id" class="border-t border-line">
                                <td class="py-2 pr-3">{{ line.legend.name }}</td>
                                <td class="py-2 pr-3 text-right tabular-nums">{{ line.all.played }}</td>
                                <td class="py-2"><WinBar :tally="line.all" /></td>
                            </tr>
                        </tbody>
                    </table>
                </section>
            </div>

            <div class="grid gap-6 lg:grid-cols-2">
                <!-- 3. Battlefields, most losses first -->
                <section class="card grid content-start gap-3 p-5" aria-labelledby="battlefields">
                    <div class="flex flex-wrap items-center justify-between gap-3">
                        <h2 id="battlefields" class="font-display text-2xl font-bold">Battlefields</h2>
                        <div class="inline-flex overflow-hidden rounded-lg border border-line text-sm" role="group" aria-label="Quels battlefields">
                            <button
                                v-for="option in [{ id: 'mine', label: isMe ? 'Les miens' : 'Les siens' }, { id: 'theirs', label: 'Adverses' }]"
                                :key="option.id"
                                type="button"
                                class="px-3 py-1.5 font-semibold"
                                :class="battlefieldSide === option.id ? 'bg-fg text-bg' : 'text-muted hover:text-fg'"
                                :aria-pressed="battlefieldSide === option.id"
                                @click="battlefieldSide = option.id"
                            >
                                {{ option.label }}
                            </button>
                        </div>
                    </div>
                    <p class="text-sm text-muted">Les plus perdus en premier. Seules les games où le battlefield a été noté comptent.</p>
                    <p v-if="battlefieldLines.length === 0" class="text-sm text-muted">Aucun battlefield noté pour l'instant.</p>
                    <table v-else class="w-full text-sm">
                        <thead>
                            <tr class="text-left text-xs text-muted">
                                <th class="py-2 pr-3 font-medium">Battlefield</th>
                                <th class="py-2 pr-3 text-right font-medium">Défaites</th>
                                <th class="py-2 font-medium">Victoires</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="line in battlefieldLines" :key="line.battlefield.id" class="border-t border-line">
                                <td class="py-2 pr-3">{{ line.battlefield.name }}</td>
                                <td class="py-2 pr-3 text-right tabular-nums">{{ line.games.losses }} / {{ line.games.played }}</td>
                                <td class="py-2"><WinBar :tally="line.games" /></td>
                            </tr>
                        </tbody>
                    </table>
                </section>

                <!-- 4. Comebacks -->
                <section class="card grid content-start gap-3 p-5" aria-labelledby="comebacks">
                    <div>
                        <h2 id="comebacks" class="font-display text-2xl font-bold">Comebacks</h2>
                        <p class="text-sm text-muted">Games gagnées après avoir été mené de 3 points ou plus.</p>
                    </div>
                    <p v-if="stats.comebacks.length === 0" class="text-sm text-muted">Pas encore de comeback.</p>
                    <ul v-else class="divide-y divide-line">
                        <li v-for="c in stats.comebacks" :key="`${c.matchId}-${c.gameNumber}`">
                            <RouterLink
                                :to="{ name: 'match', params: { id: c.matchId } }"
                                class="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-0.5 py-2.5 hover:text-accent"
                            >
                                <span class="text-sm">
                                    Contre <b class="font-semibold">{{ c.opponentName }}</b>
                                    <span class="text-muted"> · {{ c.opponentLegend?.name }}</span>
                                </span>
                                <span class="font-display text-xl font-bold tabular-nums">
                                    {{ c.scoreWhenBehind }}–{{ c.opponentScoreWhenBehind }} → {{ c.finalScore }}–{{ c.opponentFinalScore }}
                                </span>
                                <span class="col-span-2 text-xs text-muted">Game {{ c.gameNumber }} · {{ formatDateTime(c.playedAt) }}</span>
                            </RouterLink>
                        </li>
                    </ul>
                </section>
            </div>
        </template>
    </div>
</template>
