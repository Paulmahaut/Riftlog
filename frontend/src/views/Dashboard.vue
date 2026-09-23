<script setup>
import { ref, computed, onMounted } from 'vue'
import { apiFetch } from '../api.js'
import NavBarLeft from '../components/NavBarLeft.vue'
import MatchLog from '../components/MatchLog.vue'

import HomeIcon from '../assets/HomeIcon.svg?component'
import PlayIcon from '../assets/PlayIcon.svg?component'
import DeckIcon from '../assets/DeckIcon.svg?component'
import ProfileIcon from '../assets/ProfileIcon.svg?component'

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
        const usernameResponse = await apiFetch('/api/auth/me')
        if (!usernameResponse.ok) {
            throw new Error('Impossible de récupérer le username')
        }
        const user = await usernameResponse.json()
        username.value = user?.displayName ?? 'N/A'

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


const favouriteDeck = computed(() => {
    if (!matches.value.length) return 'N/A'

    const deckCounts = {}

    for (const match of matches.value) {
        const deck = match.myDeckName

        if (deck) {
            deckCounts[deck] = (deckCounts[deck] || 0) + 1
        }
    }

    return Object.entries(deckCounts)
        .sort((a, b) => b[1] - a[1])[0]?.[0] ?? 'N/A'
})

const revengePlayer = computed(() => {
    const losses = matches.value.filter(match => match.result === 'LOSS')

    if (!losses.length) return 'N/A'

    const opponentCounts = {}

    for (const match of losses) {
        const opponent = match.opponentName

        if (opponent) {
            opponentCounts[opponent] = (opponentCounts[opponent] || 0) + 1
        }
    }

    return Object.entries(opponentCounts)
        .sort((a, b) => b[1] - a[1])[0]?.[0] ?? 'N/A'
})
</script>





<template>
    <main class="flex flex-row align-items-center bg-[var(--background)]">

        <NavBarLeft :loading="loading" :username="username" />

        <!-- Main Test -->
        <div class="flex flex-col gap-8 w-full max-h-screen p-8 overflow-y-auto bg-[var(--bg)]">
            <!-- Username -->
            <div class="size- inline-flex flex-col justify-center items-start gap-1">
                <div class="size- inline-flex justify-start items-center gap-3">
                    <div class="justify-start text-white text-3xl font-normal ">Bonjour</div>
                    <div class="justify-start text-white text-3xl font-bold ">{{ username }}</div>
                    <div class="justify-start text-white text-3xl font-normal ">!</div>
                </div>
                <div class="justify-start text-white text-base font-normal">Ready for another game ?</div>
            </div>

            <!-- New Game !! -->
            <div class="p-15 bg-[var(--background-600)] rounded-xl inline-flex justify-between items-center overflow-hidden hover:scale-102 transition duration-300 ease-in-out cursor-pointer" @click="() => $router.push('/play')">
                <div class=" flex flex-col justify-start items-start gap-1">
                    <div class="text-white text-4xl font-bold">New Game !!</div>
                    <div class="text-white text-base font-normal ">This is the begining of a new story</div>
                </div>
                <PlayIcon class="h-12 text-white" />
            </div>


            <!-- Shortcut Buttons -->
            <div class="inline-flex justify-start items-center gap-10">
                <div class="flex-1 py-4 rounded-lg inline-flex flex-col justify-center items-center gap-2.5 bg-[var(--background-300)] shadow-[10px_10px_22px_0px_rgba(255,255,255,0.25)] hover:shadow-[0_0_25px_rgba(255,255,255,0.80)] transition duration-150 ease-in-out hover:cursor-pointer overflow-hidden" @click="() => $router.push('/')">
                    <HomeIcon class="h-6 invert opacity-65" />
                    <div class="text-center justify-center text-[var(--background-700)] text-base font-bold ">Home</div>
                </div>

                <div class="flex-1 py-4 rounded-lg inline-flex flex-col justify-center items-center gap-2.5 bg-[var(--background-300)] shadow-[10px_10px_22px_0px_rgba(255,255,255,0.25)] hover:shadow-[0_0_25px_rgba(255,255,255,0.80)] transition duration-150 ease-in-out hover:cursor-pointer overflow-hidden" @click="() => $router.push('/play')">
                    <PlayIcon class="h-6 invert opacity-65" />
                    <div class="text-center justify-center text-[var(--background-700)] text-base font-bold ">Play</div>
                </div>

                <div class="flex-1 py-4 rounded-lg inline-flex flex-col justify-center items-center gap-2.5 bg-[var(--background-300)] shadow-[10px_10px_22px_0px_rgba(255,255,255,0.25)] hover:shadow-[0_0_25px_rgba(255,255,255,0.80)] transition duration-150 ease-in-out hover:cursor-pointer overflow-hidden" @click="() => $router.push('/decks')">
                    <DeckIcon class="h-6 invert opacity-65" />
                    <div class="text-center justify-center text-[var(--background-700)] text-base font-bold ">Decks</div>
                </div>

                <div class="flex-1 py-4 rounded-lg inline-flex flex-col justify-center items-center gap-2.5 bg-[var(--background-300)] shadow-[10px_10px_22px_0px_rgba(255,255,255,0.25)] hover:shadow-[0_0_25px_rgba(255,255,255,0.80)] transition duration-150 ease-in-out hover:cursor-pointer overflow-hidden" @click="() => $router.push('/settings')">
                    <ProfileIcon class="h-6 invert opacity-65" />
                    <div class="text-center justify-center text-[var(--background-700)] text-base font-bold ">Profile</div>
                </div>
            </div>


            <div class="self-stretch pt-6 inline-flex flex-col justify-start items-start gap-3.5">
                <div class="text-center justify-center text-white text-2xl font-bold">Your Stats</div>

                <!-- Fast Stats Display -->
                <div class="self-stretch py-5 inline-flex justify-between items-center overflow-hidden">
                    <!-- Win Rate -->
                    <div class="size- inline-flex flex-col justify-center items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-white text-xs font-semibold">Win rate</div>
                        <div class="text-center justify-center text-[var(--background-200)] text-xl font-bold">
                            <span v-if="loading">...</span>
                            <span v-else>{{ stats.overallWinRate * 100 }}%</span>
                        </div>
                    </div>

                    <!-- Favourite Card -->
                    <div class="size- inline-flex flex-col justify-center items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-white text-xs font-semibold">Favourite Deck</div>
                        <div class="text-center justify-center text-[var(--background-200)] text-xl font-bold">
                            <span v-if="loading">...</span>
                            <span v-else>{{ favouriteDeck }}</span>
                        </div>
                    </div>

                    <!-- Average Score -->
                    <div class="size- inline-flex flex-col justify-center items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-white text-xs font-semibold">Average score</div>
                        <div class="text-center justify-center text-[var(--background-200)] text-xl font-bold">
                            <span v-if="loading">...</span>
                            <span v-else>8</span>
                        </div>
                    </div>

                    <!-- Total Matches -->
                    <div class="size- inline-flex flex-col justify-center items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-white text-xs font-semibold">Total matches</div>
                        <div class="text-center justify-center text-[var(--background-200)] text-xl font-bold">
                            <span v-if="loading">...</span>
                            <span v-else>{{ stats.totalMatches }}</span>
                        </div>
                    </div>

                    <!-- Revenge -->
                    <div class="size- inline-flex flex-col justify-center items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-white text-xs font-semibold">Take your revenge on</div>
                        <div class="text-center justify-center text-[var(--background-200)] text-xl font-bold">
                            <span v-if="loading">...</span>
                            <span v-else>{{ revengePlayer }}</span>
                        </div>
                    </div>
                </div>

                <div class="self-stretch h-0 outline outline-offset-[-1px] outline-[var(--background-300)] rounded-full"></div>
                <div class="self-stretch inline-flex justify-between items-center">
                    <div class="text-center justify-center text-white text-lg font-bold">Your matches</div>
                    <div class="text-center justify-center text-white text-sm font-light">see more</div>
                </div>

                <MatchLog v-for="match in matches" :key="match.id" :opponent="match.opponentName" :result="match.result === 'WIN'" :score="`${match.myFinalScore} - ${match.opponentFinalScore}`" :deck="match.myDeckName" :date="match.playedAt" />
            </div>
        </div>


    </main>
</template>
