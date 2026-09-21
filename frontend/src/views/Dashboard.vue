<script setup>
import { ref, onMounted } from 'vue'
import { apiFetch, currentUser } from '../api.js'
import NavBarLeft from '../components/NavBarLeft.vue'

const stats = ref(null)
const matches = ref([])
const loading = ref(true)
const legends = ref([])
const error = ref(null)

const username = ref('')

async function load() {
    try {
        // STATS
        const statsResponse = await apiFetch('/api/stats')

        if (!statsResponse.ok) {
            throw new Error('Loading Stats failed')
        }
        stats.value = await statsResponse.json()

        // USERNAME
        username.value = currentUser?.displayName ?? 'N/A'

        // MATCHES
        const matchesResponse = await apiFetch('/api/matches')
        if (!matchesResponse.ok) {
            throw new Error('Loading Matches failed')
        }
        matches.value = await matchesResponse.json()

        // LEGENDS
        const legendsResponse = await apiFetch('/api/legends')
        if (!legendsResponse.ok) {
            throw new Error('Loading Legends failed')
        }
        legends.value = await legendsResponse.json()

    } catch (e) {
        error.value = e.message
    } finally {
        loading.value = false
    }
}

onMounted(load)
</script>





<template>
    <main class="flex flex-row align-items-center bg-[var(--background)]">

        <NavBarLeft :loading="loading" :username="username" />

        <!-- Main Test -->
        <div class="flex flex-col gap-16 w-full max-h-screen p-8 overflow-y-auto bg-[var(--bg)]">
            <!-- Username -->
            <div>
                <h1 class="text-3xl font-bold">Username:</h1>
                {{ username }}
            </div>

            <!-- Stats -->
            <div v-if="stats">
                <h1 class="text-3xl font-bold">Stats:</h1>
                <pre>{{ JSON.stringify(stats, null, 2) }}</pre>
            </div>

            <!-- Matches -->
            <div>
                <h1 class="text-3xl font-bold">Matches:</h1>
                <pre v-for="match in matches" :key="match.id">{{ JSON.stringify(match, null, 2) }}</pre>
            </div>

            <!-- Legends -->
            <div>
                <h1 class="text-3xl font-bold">Legends:</h1>
                <pre v-for="legend in legends" :key="legend.id">{{ JSON.stringify(legend, null, 2) }}</pre>
            </div>

            <!-- Error -->
            <div v-if="error">
                {{ error }}
            </div>

        </div>


    </main>
</template>
