<script setup>
// History with filters: mine or the whole group, format, a legend (on either side), result.
import { computed, ref } from "vue";
import MatchList from "../components/MatchList.vue";
import { FORMAT_LABEL, outcomeFor } from "../lib/format.js";
import { session } from "../lib/session.js";

const scope = ref("mine");
const format = ref("");
const legendId = ref("");
const result = ref("");
const loaded = ref([]);

const userId = computed(() => (scope.value === "mine" ? session.user?.id ?? null : null));

/** Legends that appear in the loaded matches, for the filter. */
const legends = computed(() => {
    const byId = new Map();
    for (const match of loaded.value) {
        for (const player of match.players) {
            if (player.legend) byId.set(player.legend.id, player.legend.name);
        }
    }
    return [...byId].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
});

const filter = computed(() => {
    if (!format.value && !legendId.value && !result.value) return null;
    return (match) =>
        (!format.value || match.format === format.value) &&
        (!legendId.value || match.players.some((p) => p.legend?.id === legendId.value)) &&
        (!result.value || outcomeFor(match, session.user.id) === result.value);
});

const activeFilters = computed(() => Boolean(format.value || legendId.value || result.value));
function resetFilters() {
    format.value = "";
    legendId.value = "";
    result.value = "";
}
</script>

<template>
    <div class="mx-auto grid max-w-3xl gap-5 px-4 py-6 md:px-8 md:py-10">
        <div class="flex flex-wrap items-center justify-between gap-3">
            <h1 class="font-display text-3xl font-bold tracking-wide">Matchs</h1>
            <div class="inline-flex overflow-hidden rounded-lg border border-line" role="group" aria-label="Quels matchs afficher">
                <button
                    v-for="option in [{ id: 'mine', label: 'Les miens' }, { id: 'all', label: 'Tout le groupe' }]"
                    :key="option.id"
                    type="button"
                    class="px-4 py-2 text-sm font-semibold"
                    :class="scope === option.id ? 'bg-fg text-bg' : 'text-muted hover:text-fg'"
                    :aria-pressed="scope === option.id"
                    @click="scope = option.id"
                >
                    {{ option.label }}
                </button>
            </div>
        </div>

        <div class="flex flex-wrap items-end gap-3" role="search" aria-label="Filtres">
            <label class="field min-w-28">
                <span>Format</span>
                <select v-model="format" class="input">
                    <option value="">Tous</option>
                    <option v-for="(label, id) in FORMAT_LABEL" :key="id" :value="id">{{ label }}</option>
                </select>
            </label>
            <label class="field min-w-56 flex-1">
                <span>Légende (d'un côté ou de l'autre)</span>
                <select v-model="legendId" class="input">
                    <option value="">Toutes</option>
                    <option v-for="legend in legends" :key="legend.id" :value="legend.id">{{ legend.name }}</option>
                </select>
            </label>
            <label class="field min-w-32">
                <span>Mon résultat</span>
                <select v-model="result" class="input">
                    <option value="">Tous</option>
                    <option value="win">Victoires</option>
                    <option value="loss">Défaites</option>
                    <option value="draw">Nuls</option>
                </select>
            </label>
            <button v-if="activeFilters" class="btn" type="button" @click="resetFilters">Effacer</button>
        </div>

        <MatchList :user-id="userId" :filter="filter" @loaded="loaded = $event" />
    </div>
</template>
