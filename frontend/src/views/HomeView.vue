<script setup>
import AppIcon from "../components/AppIcon.vue";
import MatchList from "../components/MatchList.vue";
import { FORMAT_LABEL } from "../lib/format.js";
import { outboxState } from "../lib/outbox.js";
import { session } from "../lib/session.js";
import { network, syncNow } from "../lib/sync.js";
</script>

<template>
    <div class="mx-auto grid max-w-3xl gap-6 px-4 py-6 md:px-8 md:py-10">
        <h1 class="font-display text-3xl font-bold tracking-wide">Salut {{ session.user?.displayName }}</h1>

        <RouterLink
            to="/play"
            class="card flex items-center justify-between gap-4 p-5 transition-colors hover:bg-surface-2"
        >
            <div>
                <p class="label">Nouvelle partie</p>
                <p class="mt-1 font-display text-2xl font-bold">Lancer le compteur</p>
                <p class="mt-1 text-sm text-muted">
                    {{ FORMAT_LABEL[session.user?.defaultFormat ?? "BO3"] }} par défaut · fonctionne sans réseau
                </p>
            </div>
            <span class="grid size-14 shrink-0 place-items-center rounded-full bg-fg text-bg">
                <AppIcon name="play" class="size-6" />
            </span>
        </RouterLink>

        <section
            v-if="outboxState.pending > 0"
            class="card flex flex-wrap items-center justify-between gap-3 p-4"
            aria-labelledby="pending-title"
        >
            <div>
                <p id="pending-title" class="font-semibold">
                    {{ outboxState.pending }} match{{ outboxState.pending > 1 ? "s" : "" }} à envoyer
                </p>
                <p class="text-sm text-muted">
                    <template v-if="outboxState.pending > 1">
                        {{ network.online ? "Ils partent automatiquement." : "Ils partiront au retour du réseau." }}
                    </template>
                    <template v-else>
                        {{ network.online ? "Il part automatiquement." : "Il partira au retour du réseau." }}
                    </template>
                </p>
            </div>
            <button class="btn" type="button" :disabled="!network.online || outboxState.syncing" @click="syncNow()">
                <AppIcon name="upload" class="size-4" />
                Envoyer maintenant
            </button>
        </section>

        <section class="grid gap-3" aria-labelledby="recent-title">
            <div class="flex items-baseline justify-between">
                <h2 id="recent-title" class="label">Mes derniers matchs</h2>
                <RouterLink to="/matches" class="text-sm font-semibold text-accent">Tout voir</RouterLink>
            </div>
            <MatchList
                :user-id="session.user?.id ?? null"
                :limit="5"
                empty-text="Aucun match pour l'instant. Lance ta première partie avec le compteur."
            />
        </section>
    </div>
</template>
