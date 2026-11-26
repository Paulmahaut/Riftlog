<script setup>
import { computed, ref } from "vue";
import MatchList from "../components/MatchList.vue";
import { session } from "../lib/session.js";

const scope = ref("mine");
const userId = computed(() => (scope.value === "mine" ? session.user?.id ?? null : null));
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
        <MatchList :user-id="userId" />
        <p class="text-xs text-muted">Le détail d'un match, point par point, et les statistiques arrivent à l'étape 4.</p>
    </div>
</template>
