<script setup>
// Small pill telling whether everything recorded on this device has reached the server.
import { computed } from "vue";
import AppIcon from "./AppIcon.vue";
import { outboxState } from "../lib/outbox.js";
import { network } from "../lib/sync.js";

const state = computed(() => {
    if (outboxState.syncing) return { icon: "upload", text: "Envoi…", tone: "text-muted" };
    if (outboxState.pending > 0) {
        const n = outboxState.pending;
        return { icon: "upload", text: `${n} en attente`, tone: "text-warning" };
    }
    if (!network.online) return { icon: "offline", text: "Hors-ligne", tone: "text-muted" };
    return { icon: "check", text: "À jour", tone: "text-success" };
});
</script>

<template>
    <RouterLink
        :to="{ name: 'settings', hash: '#envois' }"
        class="inline-flex items-center gap-1.5 text-xs font-medium"
        :class="state.tone"
        :title="'Synchronisation : ' + state.text"
    >
        <AppIcon :name="state.icon" class="size-4" />
        {{ state.text }}
    </RouterLink>
</template>
