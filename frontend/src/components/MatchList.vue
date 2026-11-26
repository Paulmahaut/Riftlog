<script setup>
// Loads and lists matches from the server, with loading, offline and empty states.
import { computed, onMounted, ref, watch } from "vue";
import MatchRow from "./MatchRow.vue";
import { api, describeError, NetworkError } from "../lib/api.js";
import { network } from "../lib/sync.js";

const props = defineProps({
    /** Only matches this user played in; null = the whole group. */
    userId: { type: Number, default: null },
    limit: { type: Number, default: 0 },
    emptyText: { type: String, default: "Aucun match pour l'instant." },
    /** Optional (match) => boolean, applied after loading. */
    filter: { type: Function, default: null },
    filteredEmptyText: { type: String, default: "Aucun match ne correspond à ces filtres." },
});
const emit = defineEmits(["loaded"]);

const matches = ref([]);
const loading = ref(true);
const error = ref("");
const offline = ref(false);

async function load() {
    loading.value = true;
    error.value = "";
    offline.value = false;
    try {
        const all = await api(props.userId == null ? "/api/matches" : `/api/matches?userId=${props.userId}`);
        matches.value = props.limit ? all.slice(0, props.limit) : all;
        emit("loaded", matches.value);
    } catch (e) {
        offline.value = e instanceof NetworkError && !network.online;
        error.value = describeError(e);
    } finally {
        loading.value = false;
    }
}

onMounted(load);
watch(() => props.userId, load);
// Back online after a failed load: try again without waiting for a tap.
watch(
    () => network.online,
    (online) => online && (error.value || offline.value) && load(),
);
defineExpose({ reload: load });

const shown = computed(() => (props.filter ? matches.value.filter(props.filter) : matches.value));
</script>

<template>
    <div class="card overflow-hidden">
        <p v-if="loading" class="px-4 py-6 text-sm text-muted">Chargement…</p>
        <p v-else-if="offline" class="px-4 py-6 text-sm text-muted">
            L'historique s'affichera au retour du réseau.
        </p>
        <div v-else-if="error" class="flex flex-wrap items-center justify-between gap-3 px-4 py-4 text-sm">
            <span class="text-muted">{{ error }}</span>
            <button class="btn" type="button" @click="load">Réessayer</button>
        </div>
        <p v-else-if="matches.length === 0" class="px-4 py-6 text-sm text-muted">{{ emptyText }}</p>
        <p v-else-if="shown.length === 0" class="px-4 py-6 text-sm text-muted">{{ filteredEmptyText }}</p>
        <ul v-else class="divide-y divide-line">
            <li v-for="match in shown" :key="match.id">
                <MatchRow :match="match" />
            </li>
        </ul>
    </div>
</template>
