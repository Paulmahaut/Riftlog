<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AuthLayout from "../components/AuthLayout.vue";
import { ApiError, describeError } from "../lib/api.js";
import { login } from "../lib/auth.js";
import { syncNow } from "../lib/sync.js";

const route = useRoute();
const router = useRouter();

const email = ref(typeof route.query.email === "string" ? route.query.email : "");
const password = ref("");
const error = ref("");
const busy = ref(false);
const sessionExpired = Boolean(route.query.again);

async function submit() {
    error.value = "";
    busy.value = true;
    try {
        await login(email.value, password.value);
        syncNow({ forceReference: true });
        const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/";
        router.replace(redirect);
    } catch (e) {
        error.value = e instanceof ApiError && e.status === 401 ? "Email ou mot de passe incorrect." : describeError(e);
    } finally {
        busy.value = false;
    }
}
</script>

<template>
    <AuthLayout
        title="Riftlog"
        :subtitle="sessionExpired ? 'Reconnecte-toi pour envoyer tes matchs en attente.' : 'Le compagnon de vos parties de Riftbound.'"
    >
        <form class="card grid gap-4 p-5" @submit.prevent="submit">
            <label class="field">
                <span>Email</span>
                <input id="login-email" v-model="email" class="input" type="email" autocomplete="email" required />
            </label>
            <label class="field">
                <span>Mot de passe</span>
                <input
                    id="login-password"
                    v-model="password"
                    class="input"
                    type="password"
                    autocomplete="current-password"
                    required
                />
            </label>
            <p v-if="error" class="text-sm text-danger" role="alert">{{ error }}</p>
            <button class="btn btn-primary" type="submit" :disabled="busy">
                {{ busy ? "Connexion…" : "Se connecter" }}
            </button>
        </form>
        <p class="text-center text-sm text-muted">
            Pas encore de compte ?
            <RouterLink to="/register" class="font-semibold text-fg underline">Créer un compte</RouterLink>
        </p>
    </AuthLayout>
</template>
