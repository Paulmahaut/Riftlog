<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import AuthLayout from "../components/AuthLayout.vue";
import { ApiError, describeError } from "../lib/api.js";
import { register } from "../lib/auth.js";
import { syncNow } from "../lib/sync.js";

const router = useRouter();

const displayName = ref("");
const email = ref("");
const password = ref("");
const error = ref("");
const busy = ref(false);

const passwordTooShort = computed(() => password.value.length > 0 && password.value.length < 8);

async function submit() {
    error.value = "";
    if (password.value.length < 8) {
        error.value = "Le mot de passe doit faire au moins 8 caractères.";
        return;
    }
    busy.value = true;
    try {
        await register(displayName.value, email.value, password.value);
        syncNow({ forceReference: true });
        router.replace("/");
    } catch (e) {
        error.value = e instanceof ApiError && e.status === 409 ? "Un compte existe déjà avec cet email." : describeError(e);
    } finally {
        busy.value = false;
    }
}
</script>

<template>
    <AuthLayout title="Créer un compte" subtitle="Tes amis verront ton nom dans leurs matchs.">
        <form class="card grid gap-4 p-5" @submit.prevent="submit">
            <label class="field">
                <span>Pseudo</span>
                <input
                    id="register-name"
                    v-model="displayName"
                    class="input"
                    autocomplete="nickname"
                    maxlength="40"
                    required
                />
            </label>
            <label class="field">
                <span>Email</span>
                <input id="register-email" v-model="email" class="input" type="email" autocomplete="email" required />
            </label>
            <label class="field">
                <span>Mot de passe</span>
                <input
                    id="register-password"
                    v-model="password"
                    class="input"
                    type="password"
                    autocomplete="new-password"
                    required
                />
                <small class="text-xs" :class="passwordTooShort ? 'text-danger' : 'text-muted'">8 caractères minimum</small>
            </label>
            <p v-if="error" class="text-sm text-danger" role="alert">{{ error }}</p>
            <button class="btn btn-primary" type="submit" :disabled="busy">
                {{ busy ? "Création…" : "Créer mon compte" }}
            </button>
        </form>
        <p class="text-center text-sm text-muted">
            Déjà inscrit ?
            <RouterLink to="/login" class="font-semibold text-fg underline">Se connecter</RouterLink>
        </p>
    </AuthLayout>
</template>
