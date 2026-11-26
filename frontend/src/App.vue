<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import AppShell from "./components/AppShell.vue";

const route = useRoute();
// Login pages and the duel screen have no navigation around them.
const bare = computed(() => route.meta.public || route.meta.fullscreen);
</script>

<template>
    <RouterView v-if="bare" />
    <AppShell v-else>
        <RouterView />
    </AppShell>
</template>
