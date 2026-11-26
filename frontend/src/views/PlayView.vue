<script setup>
// Placeholder until the duel screen (step 3). It already shows that everything the
// duel needs offline is cached on this device.
import { onMounted, ref } from "vue";
import { cachedPlayers, referenceStatus } from "../lib/reference.js";
import { formatRelative } from "../lib/format.js";

const status = ref(null);
const players = ref(0);

onMounted(async () => {
    status.value = (await referenceStatus()) ?? null;
    players.value = (await cachedPlayers()).length;
});
</script>

<template>
    <div class="mx-auto grid max-w-xl gap-5 px-4 py-6 md:px-8 md:py-10">
        <h1 class="font-display text-3xl font-bold tracking-wide">Jouer</h1>
        <div class="card grid gap-3 p-5">
            <p class="font-semibold">L'écran de duel arrive à l'étape 3.</p>
            <p class="text-sm text-muted">
                Il reprendra la maquette validée : compteur Conquer / Hold / Ability, Bo1 ou Bo3, chrono, puis
                l'écran d'après-partie. Les matchs seront gardés sur le téléphone et envoyés automatiquement.
            </p>
        </div>
        <div class="card grid gap-2 p-5">
            <p class="label">Prêt pour jouer hors-ligne</p>
            <template v-if="status">
                <p class="text-sm">
                    {{ status.legends }} légendes, {{ status.battlefields }} battlefields et {{ players }} joueurs
                    sont enregistrés sur cet appareil.
                </p>
                <p class="text-xs text-muted">Mis à jour {{ formatRelative(status.syncedAt) }}.</p>
            </template>
            <p v-else class="text-sm text-muted">
                Les données de jeu ne sont pas encore sur cet appareil. Connecte-toi une fois avec du réseau.
            </p>
        </div>
    </div>
</template>
