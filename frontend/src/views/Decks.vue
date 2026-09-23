<script setup>
import { ref, onMounted } from 'vue'
import { apiFetch } from '../api.js'
import NavBarLeft from '../components/NavBarLeft.vue'

const loading = ref(true)
const error = ref(null)
const creating = ref(false)

const username = ref('')

const decks = ref([])
const legends = ref([])

const showCreateForm = ref(false)

const newDeck = ref({
    name: '',
    legendName: ''
})

async function load() {
    loading.value = true
    error.value = null

    try {
        // =========================
        // USERNAME
        // =========================
        const userResponse = await apiFetch('/api/auth/me')

        if (!userResponse.ok) {
            throw new Error('Impossible de récupérer le username')
        }

        const user = await userResponse.json()
        username.value = user?.displayName ?? 'N/A'


        // =========================
        // DECKS
        // =========================
        const decksResponse = await apiFetch('/api/decks')

        if (!decksResponse.ok) {
            throw new Error('Impossible de récupérer les decks')
        }

        decks.value = await decksResponse.json()


        // =========================
        // LEGENDS
        // =========================
        const legendsResponse = await apiFetch('/api/legends')

        if (!legendsResponse.ok) {
            throw new Error('Impossible de récupérer les légendes')
        }

        legends.value = await legendsResponse.json()

    } catch (e) {
        error.value = e.message
    } finally {
        loading.value = false
    }
}


// =========================
// CREATE DECK
// =========================

async function createDeck() {
    error.value = null

    const name = newDeck.value.name.trim()
    const legendName = newDeck.value.legendName.trim()

    if (!name || !legendName) {
        error.value = 'Veuillez remplir tous les champs'
        return
    }

    creating.value = true

    try {
        const response = await apiFetch('/api/decks', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name,
                legendName
            })
        })

        if (!response.ok) {
            const message = await response.text()
            throw new Error(
                message || 'Impossible de créer le deck'
            )
        }

        const createdDeck = await response.json()

        // Ajouter directement le deck à la liste
        decks.value.push(createdDeck)

        // Reset formulaire
        newDeck.value = {
            name: '',
            legendName: ''
        }

        showCreateForm.value = false

    } catch (e) {
        error.value = e.message
    } finally {
        creating.value = false
    }
}


function cancelCreate() {
    newDeck.value = {
        name: '',
        legendName: ''
    }

    showCreateForm.value = false
}

// =========================
// CREATE LEGEND
// =========================

const showCreateLegendForm = ref(false)
const creatingLegend = ref(false)

const newLegend = ref({
    name: ''
})

async function createLegend() {
    error.value = null

    const name = newLegend.value.name.trim()

    if (!name) {
        error.value = 'Veuillez entrer un nom de légende'
        return
    }

    creatingLegend.value = true

    try {
        const response = await apiFetch('/api/legends', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name
            })
        })

        if (!response.ok) {
            const message = await response.text()
            throw new Error(
                message || 'Impossible de créer la légende'
            )
        }

        const createdLegend = await response.json()

        // Ajoute directement la légende à la liste
        legends.value.push(createdLegend)

        // Reset
        newLegend.value = {
            name: ''
        }

        showCreateLegendForm.value = false

    } catch (e) {
        error.value = e.message
    } finally {
        creatingLegend.value = false
    }
}

function cancelCreateLegend() {
    newLegend.value = {
        name: ''
    }

    showCreateLegendForm.value = false
}

onMounted(load)
</script>


<template>

    <main class="flex flex-row min-h-screen bg-[var(--background)]">

        <NavBarLeft :loading="loading" :username="username" />


        <!-- MAIN -->
        <div class="flex flex-col gap-10 w-full max-h-screen p-8 overflow-y-auto bg-[var(--bg)]">

            <!-- HEADER buttons -->
            <div class="flex items-center justify-between">

                <div>
                    <h1 class="text-3xl font-bold">
                        Decks
                    </h1>

                    <p class="mt-2 text-sm opacity-60">
                        {{ decks.length }} deck{{ decks.length > 1 ? 's' : '' }}
                    </p>
                </div>

                <div class="flex gap-3">
                    <button @click="showCreateLegendForm = true" class="px-5 py-3 rounded-lg bg-[var(--background-700)] text-white/90 font-medium hover:opacity-80 transition">
                        + Create Legend
                    </button>

                    <button @click="showCreateForm = true" class="px-5 py-3 rounded-lg bg-[var(--background-900)] text-white font-medium hover:opacity-80 transition">
                        + Create Deck
                    </button>
                </div>

            </div>


            <!-- ERROR -->
            <div v-if="error" class="bg-red-500/90 text-white px-5 py-3 rounded-lg shadow-lg text-sm font-medium">
                {{ error }}
            </div>


            <!-- LOADING -->
            <div v-if="loading" class="flex justify-center py-20 opacity-60">
                Loading decks...
            </div>


            <!-- EMPTY -->
            <div v-else-if="decks.length === 0" class="flex flex-col items-center justify-center py-20 text-center">
                <p class="text-lg font-semibold">
                    No decks yet
                </p>

                <p class="mt-2 text-sm opacity-60">
                    Create your first deck to get started.
                </p>

                <button @click="showCreateForm = true" class="mt-5 px-5 py-3 rounded-lg bg-[var(--background-700)] text-white">
                    Create a deck
                </button>

            </div>


            <!-- DECKS -->
            <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
                <div v-for="deck in decks" :key="deck.id" class="flex flex-col gap-4 p-5 rounded-xl bg-[var(--background-900)] shadow">

                    <!-- Deck name -->
                    <div>
                        <h2 class="text-xl font-bold text-[var(--background-300)]">
                            {{ deck.name }}
                        </h2>

                        <p class="mt-1 text-sm opacity-60">
                            Deck #{{ deck.id }}
                        </p>
                    </div>


                    <!-- Legend -->
                    <div class="flex flex-col gap-2">
                        <span class="text-sm self-start italic font-bold">
                            Legends
                        </span>

                        <div class="flex items-center justify-between p-3 rounded-lg bg-[var(--background-700)]">
                            <span class="font-medium">
                                {{ deck.legendName }}
                            </span>
                        </div>
                    </div>

                </div>

            </div>
        </div>


        <!-- CREATE LEGEND MODAL -->
        <div v-if="showCreateLegendForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60" @click.self="cancelCreateLegend">

            <div class="w-full max-w-md p-6 rounded-xl bg-[var(--background-200)] shadow-2xl">

                <!-- Header -->
                <div class="flex items-center justify-between mb-6">

                    <h2 class="text-2xl font-bold">
                        Create a Legend
                    </h2>

                    <button @click="cancelCreateLegend" class="text-xl opacity-60 hover:opacity-100">
                        x
                    </button>

                </div>


                <!-- Name -->
                <div class="flex flex-col gap-2 mb-6">

                    <label class="text-sm font-medium">
                        Legend name
                    </label>

                    <input v-model="newLegend.name" type="text" placeholder="Jiro" class="w-full p-4 rounded-lg bg-[var(--background-300)] outline-none" @keyup.enter="createLegend" />

                </div>

                <!-- Error -->
                <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-500/20 text-red-400 text-sm">
                    {{ error }}
                </div>

                <!-- Buttons -->
                <div class="flex gap-3">

                    <button @click="cancelCreateLegend" :disabled="creatingLegend" class="flex-1 px-4 py-3 rounded-lg bg-[var(--background-300)] hover:opacity-80">
                        Cancel
                    </button>

                    <button @click="createLegend" :disabled="creatingLegend" class="flex-1 px-4 py-3 rounded-lg bg-[var(--background-700)] text-white hover:opacity-80 disabled:opacity-50">
                        {{ creatingLegend ? 'Creating...' : 'Create Legend' }}
                    </button>
                </div>
            </div>

        </div>

        <!-- CREATE DECK MODAL -->
        <div v-if="showCreateForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60" @click.self="cancelCreate">
            <div class="w-full max-w-md p-6 rounded-xl bg-[var(--background-200)] shadow-2xl">

                <!-- Modal header -->
                <div class="flex items-center justify-between mb-6">

                    <h2 class="text-2xl font-bold">
                        Create a deck
                    </h2>

                    <button @click="cancelCreate" class="text-xl opacity-60 hover:opacity-100">
                        x
                    </button>

                </div>


                <!-- Deck name -->
                <div class="flex flex-col gap-2 mb-5">

                    <label class="text-sm font-medium">
                        Deck name
                    </label>

                    <input v-model="newDeck.name" type="text" placeholder="My awesome deck" class="w-full p-4 rounded-lg bg-[var(--background-300)] outline-none" @keyup.enter="createDeck" />

                </div>


                <!-- Legend -->
                <div class="flex flex-col gap-2 mb-6">

                    <label class="text-sm font-medium">
                        Legend
                    </label>

                    <select v-model="newDeck.legendName" class="w-full p-4 rounded-lg bg-[var(--background-300)] outline-none">

                        <option value="" disabled>
                            Select a legend
                        </option>

                        <option v-for="legend in legends" :key="legend.id" :value="legend.name">
                            {{ legend.name }}
                        </option>

                    </select>

                </div>


                <!-- Error inside modal -->
                <div v-if="error" class="mb-5 p-3 rounded-lg bg-red-500/20 text-red-400 text-sm">
                    {{ error }}
                </div>


                <!-- Buttons -->
                <div class="flex gap-3">

                    <button @click="cancelCreate" :disabled="creating" class="flex-1 px-4 py-3 rounded-lg bg-[var(--background-300)] hover:opacity-80">
                        Cancel
                    </button>


                    <button @click="createDeck" :disabled="creating" class="flex-1 px-4 py-3 rounded-lg bg-[var(--background-700)] text-white hover:opacity-80 disabled:opacity-50">
                        {{ creating ? 'Creating...' : 'Create deck' }}
                    </button>

                </div>

            </div>

        </div>

    </main>

</template>