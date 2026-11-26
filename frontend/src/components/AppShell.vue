<script setup>
// Layout for every logged-in page: sidebar on PC (md and up), bottom tab bar on phones.
// Status banners (offline, expired session, refused uploads) sit above the page content.
import { computed } from "vue";
import { useRoute } from "vue-router";
import AppIcon from "./AppIcon.vue";
import SyncBadge from "./SyncBadge.vue";
import { outboxState } from "../lib/outbox.js";
import { session } from "../lib/session.js";
import { network } from "../lib/sync.js";

const route = useRoute();

// mobile: false = PC sidebar only (5 tabs is the most a phone bar holds comfortably).
const NAV = [
    { to: "/", name: "home", label: "Accueil", icon: "home" },
    { to: "/matches", name: "matches", label: "Matchs", icon: "matches" },
    { to: "/play", name: "play", label: "Jouer", icon: "play", primary: true },
    { to: "/stats", name: "stats", label: "Stats", icon: "stats" },
    { to: "/decks", name: "decks", label: "Decks", icon: "decks", mobile: false },
    { to: "/settings", name: "settings", label: "Réglages", icon: "settings" },
];
const MOBILE_NAV = NAV.filter((item) => item.mobile !== false);

/** A sub-page (a match's detail) keeps its section highlighted. */
const section = computed(() => route.meta.section ?? route.name);

const loginAgain = computed(() => ({
    name: "login",
    query: { again: "1", email: session.user?.email, redirect: route.fullPath },
}));
</script>

<template>
    <div class="flex h-full">
        <!-- PC: sidebar -->
        <aside class="hidden w-60 shrink-0 flex-col border-r border-line bg-surface md:flex">
            <div class="flex items-center gap-3 px-5 py-6">
                <img src="/favicon.svg" alt="" class="size-8" />
                <span class="font-display text-2xl font-bold tracking-wide">Riftlog</span>
            </div>
            <nav class="grid gap-1 px-3" aria-label="Navigation principale">
                <RouterLink
                    v-for="item in NAV"
                    :key="item.name"
                    :to="item.to"
                    class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted hover:bg-surface-2 hover:text-fg"
                    :class="{ '!bg-surface-2 !text-fg': section === item.name }"
                    :aria-current="section === item.name ? 'page' : undefined"
                >
                    <AppIcon :name="item.icon" class="size-5" />
                    {{ item.label }}
                </RouterLink>
            </nav>
            <div class="mt-auto grid gap-2 border-t border-line px-5 py-4 text-sm">
                <span class="truncate font-semibold">{{ session.user?.displayName }}</span>
                <SyncBadge />
            </div>
        </aside>

        <div class="flex min-w-0 flex-1 flex-col">
            <!-- Phone: top bar -->
            <header
                class="flex items-center justify-between border-b border-line bg-surface px-4 py-3 md:hidden"
                style="padding-top: max(0.75rem, env(safe-area-inset-top))"
            >
                <span class="font-display text-xl font-bold tracking-wide">Riftlog</span>
                <SyncBadge />
            </header>

            <div class="grid gap-px">
                <div v-if="!network.online" class="flex items-center gap-2 bg-surface-2 px-4 py-2 text-sm text-muted" role="status">
                    <AppIcon name="offline" class="size-4 shrink-0" />
                    Hors-ligne. Tes parties restent sur cet appareil et partiront au retour du réseau.
                </div>
                <div v-if="session.expired" class="flex flex-wrap items-center gap-x-3 gap-y-1 bg-surface-2 px-4 py-2 text-sm" role="alert">
                    <AppIcon name="alert" class="size-4 shrink-0 text-warning" />
                    <span>Ta session a expiré : tes matchs attendent que tu te reconnectes.</span>
                    <RouterLink :to="loginAgain" class="font-semibold text-accent underline">Me reconnecter</RouterLink>
                </div>
                <div v-if="outboxState.rejected > 0" class="flex flex-wrap items-center gap-x-3 gap-y-1 bg-surface-2 px-4 py-2 text-sm" role="alert">
                    <AppIcon name="alert" class="size-4 shrink-0 text-danger" />
                    <span>
                        {{ outboxState.rejected }} match{{ outboxState.rejected > 1 ? "s" : "" }}
                        refusé{{ outboxState.rejected > 1 ? "s" : "" }} par le serveur.
                    </span>
                    <RouterLink :to="{ name: 'settings', hash: '#envois' }" class="font-semibold text-accent underline">Voir pourquoi</RouterLink>
                </div>
            </div>

            <main class="min-h-0 flex-1 overflow-y-auto">
                <slot />
            </main>

            <!-- Phone: bottom tab bar -->
            <nav
                class="grid grid-cols-5 border-t border-line bg-surface md:hidden"
                style="padding-bottom: env(safe-area-inset-bottom)"
                aria-label="Navigation principale"
            >
                <RouterLink
                    v-for="item in MOBILE_NAV"
                    :key="item.name"
                    :to="item.to"
                    class="flex flex-col items-center gap-1 py-2 text-[11px] font-medium text-muted"
                    :class="{ '!text-fg': section === item.name }"
                    :aria-current="section === item.name ? 'page' : undefined"
                >
                    <span
                        class="grid size-8 place-items-center"
                        :class="item.primary ? 'rounded-full bg-fg text-bg' : ''"
                    >
                        <AppIcon :name="item.icon" :class="item.primary ? 'size-4' : 'size-6'" />
                    </span>
                    {{ item.label }}
                </RouterLink>
            </nav>
        </div>
    </div>
</template>
