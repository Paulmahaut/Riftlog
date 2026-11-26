<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppIcon from "../components/AppIcon.vue";
import { describeError } from "../lib/api.js";
import { logout, saveDefaultFormat } from "../lib/auth.js";
import { FORMAT_LABEL, formatDateTime, formatRelative } from "../lib/format.js";
import { discardItem, outboxItems, outboxState, retryItem } from "../lib/outbox.js";
import { referenceStatus } from "../lib/reference.js";
import { session } from "../lib/session.js";
import { network, syncNow } from "../lib/sync.js";
import { THEMES, applyTheme, currentTheme } from "../lib/theme.js";

const router = useRouter();

const formatError = ref("");
const savingFormat = ref(false);
const reference = ref(null);
const refreshing = ref(false);
const items = ref([]);
const confirmDiscard = ref(null);
const confirmLogout = ref(false);

async function loadLocalData() {
    reference.value = (await referenceStatus()) ?? null;
    items.value = session.user ? await outboxItems(session.user.id) : [];
}

onMounted(loadLocalData);

async function chooseFormat(format) {
    if (format === session.user.defaultFormat) return;
    formatError.value = "";
    savingFormat.value = true;
    try {
        await saveDefaultFormat(format);
    } catch (e) {
        formatError.value = describeError(e);
    } finally {
        savingFormat.value = false;
    }
}

async function refreshNow() {
    refreshing.value = true;
    await syncNow({ forceReference: true });
    await loadLocalData();
    refreshing.value = false;
}

async function retry(id) {
    await retryItem(session.user.id, id);
    await syncNow();
    await loadLocalData();
}

async function discard(id) {
    await discardItem(session.user.id, id);
    confirmDiscard.value = null;
    await loadLocalData();
}

function doLogout() {
    logout();
    router.replace("/login");
}

function matchSummary(body) {
    const games = body?.games?.length ?? 0;
    return `${FORMAT_LABEL[body?.format] ?? "Match"} · ${games} game${games > 1 ? "s" : ""}`;
}
</script>

<template>
    <div class="mx-auto grid max-w-2xl gap-6 px-4 py-6 md:px-8 md:py-10">
        <h1 class="font-display text-3xl font-bold tracking-wide">Réglages</h1>

        <section class="card grid gap-1 p-5" aria-labelledby="profile-title">
            <h2 id="profile-title" class="label">Profil</h2>
            <p class="text-lg font-semibold">{{ session.user?.displayName }}</p>
            <p class="text-sm text-muted">{{ session.user?.email }}</p>
        </section>

        <section class="card grid gap-3 p-5" aria-labelledby="format-title">
            <div>
                <h2 id="format-title" class="label">Format par défaut</h2>
                <p class="mt-1 text-sm text-muted">Proposé à chaque nouvelle partie, modifiable au lancement.</p>
            </div>
            <div class="inline-flex w-fit overflow-hidden rounded-lg border border-line" role="group" aria-label="Format par défaut">
                <button
                    v-for="format in ['BO1', 'BO3']"
                    :key="format"
                    type="button"
                    class="px-5 py-2 text-sm font-semibold"
                    :class="session.user?.defaultFormat === format ? 'bg-fg text-bg' : 'text-muted hover:text-fg'"
                    :aria-pressed="session.user?.defaultFormat === format"
                    :disabled="savingFormat || !network.online"
                    @click="chooseFormat(format)"
                >
                    {{ FORMAT_LABEL[format] }}
                </button>
            </div>
            <p v-if="!network.online" class="text-xs text-muted">Il faut du réseau pour changer ce réglage.</p>
            <p v-if="formatError" class="text-sm text-danger" role="alert">{{ formatError }}</p>
        </section>

        <section class="card grid gap-3 p-5" aria-labelledby="theme-title">
            <h2 id="theme-title" class="label">Thème</h2>
            <div class="grid gap-2 sm:grid-cols-2">
                <button
                    v-for="theme in THEMES"
                    :key="theme.id"
                    type="button"
                    class="grid gap-1 rounded-xl border p-3 text-left"
                    :class="currentTheme === theme.id ? 'border-fg' : 'border-line hover:border-muted'"
                    :aria-pressed="currentTheme === theme.id"
                    @click="applyTheme(theme.id)"
                >
                    <span class="font-semibold">{{ theme.label }}</span>
                    <span class="text-xs text-muted">{{ theme.description }}</span>
                </button>
            </div>
            <p class="text-xs text-muted">Un thème illustré pour PC viendra s'ajouter.</p>
        </section>

        <section class="card grid gap-3 p-5" aria-labelledby="offline-title">
            <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h2 id="offline-title" class="label">Données hors-ligne</h2>
                    <p v-if="reference" class="mt-1 text-sm">
                        {{ reference.legends }} légendes, {{ reference.battlefields }} battlefields,
                        {{ reference.players }} joueurs et invités.
                    </p>
                    <p v-if="reference" class="text-xs text-muted">Mis à jour {{ formatRelative(reference.syncedAt) }}.</p>
                    <p v-else class="mt-1 text-sm text-muted">Pas encore téléchargées sur cet appareil.</p>
                </div>
                <button class="btn" type="button" :disabled="refreshing || !network.online" @click="refreshNow">
                    <AppIcon name="refresh" class="size-4" />
                    {{ refreshing ? "Mise à jour…" : "Mettre à jour" }}
                </button>
            </div>
        </section>

        <section id="envois" class="card grid gap-3 p-5" aria-labelledby="outbox-title">
            <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h2 id="outbox-title" class="label">Matchs à envoyer</h2>
                    <p class="mt-1 text-sm text-muted">
                        <template v-if="items.length === 0">Tout est envoyé.</template>
                        <template v-else>Enregistrés sur cet appareil, pas encore sur le serveur.</template>
                    </p>
                </div>
                <button
                    v-if="outboxState.pending > 0"
                    class="btn"
                    type="button"
                    :disabled="!network.online || outboxState.syncing || session.expired"
                    @click="syncNow().then(loadLocalData)"
                >
                    <AppIcon name="upload" class="size-4" />
                    Envoyer
                </button>
            </div>
            <ul v-if="items.length" class="divide-y divide-line rounded-xl border border-line">
                <li v-for="item in items" :key="item.id" class="grid gap-2 p-3 text-sm">
                    <div class="flex flex-wrap items-center justify-between gap-2">
                        <span class="font-semibold">{{ matchSummary(item.body) }}</span>
                        <span class="text-xs text-muted">{{ formatDateTime(item.createdAt) }}</span>
                    </div>
                    <p v-if="item.status === 'rejected'" class="text-danger">Refusé : {{ item.lastError }}</p>
                    <p v-else-if="item.lastError" class="text-muted">Dernier essai : {{ item.lastError }}</p>
                    <div v-if="item.status === 'rejected'" class="flex flex-wrap gap-2">
                        <button class="btn" type="button" @click="retry(item.id)">Réessayer</button>
                        <template v-if="confirmDiscard === item.id">
                            <button class="btn !border-danger !text-danger" type="button" @click="discard(item.id)">
                                Supprimer définitivement
                            </button>
                            <button class="btn" type="button" @click="confirmDiscard = null">Annuler</button>
                        </template>
                        <button v-else class="btn" type="button" @click="confirmDiscard = item.id">
                            <AppIcon name="trash" class="size-4" />
                            Supprimer
                        </button>
                    </div>
                </li>
            </ul>
        </section>

        <section class="card grid gap-3 p-5" aria-labelledby="logout-title">
            <h2 id="logout-title" class="label">Session</h2>
            <template v-if="confirmLogout">
                <p v-if="items.length" class="text-sm text-warning">
                    {{ items.length }} match{{ items.length > 1 ? "s restent" : " reste" }} sur cet appareil. Ils seront
                    envoyés quand tu te reconnecteras ici.
                </p>
                <div class="flex flex-wrap gap-2">
                    <button class="btn !border-danger !text-danger" type="button" @click="doLogout">Me déconnecter</button>
                    <button class="btn" type="button" @click="confirmLogout = false">Annuler</button>
                </div>
            </template>
            <button v-else class="btn w-fit" type="button" @click="confirmLogout = true">
                <AppIcon name="logout" class="size-4" />
                Se déconnecter
            </button>
        </section>
    </div>
</template>
