<script setup>
import { ref, onMounted } from 'vue'
import { apiFetch, currentUser } from '../api.js'
import NavBarLeft from '../components/NavBarLeft.vue'

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
                <div class="flex-1 py-4 rounded-lg inline-flex flex-col justify-center items-center gap-2.5 bg-[var(--background-300)] shadow-[10px_10px_22px_0px_rgba(255,255,255,0.25)] hover:shadow-[0_0_25px_rgba(255,255,255,0.80)] transition duration-150 ease-in-out overflow-hidden" @click="() => $router.push('/')">
                    <HomeIcon class="h-6 invert opacity-65" />
                    <div class="text-center justify-center text-[var(--background-700)] text-base font-bold ">Home</div>
                </div>

                <div class="flex-1 py-4 rounded-lg inline-flex flex-col justify-center items-center gap-2.5 bg-[var(--background-300)] shadow-[10px_10px_22px_0px_rgba(255,255,255,0.25)] hover:shadow-[0_0_25px_rgba(255,255,255,0.80)] transition duration-150 ease-in-out overflow-hidden" @click="() => $router.push('/play')">
                    <PlayIcon class="h-6 invert opacity-65" />
                    <div class="text-center justify-center text-[var(--background-700)] text-base font-bold ">Play</div>
                </div>

                <div class="flex-1 py-4 rounded-lg inline-flex flex-col justify-center items-center gap-2.5 bg-[var(--background-300)] shadow-[10px_10px_22px_0px_rgba(255,255,255,0.25)] hover:shadow-[0_0_25px_rgba(255,255,255,0.80)] transition duration-150 ease-in-out overflow-hidden" @click="() => $router.push('/decks')">
                    <DeckIcon class="h-6 invert opacity-65" />
                    <div class="text-center justify-center text-[var(--background-700)] text-base font-bold ">Decks</div>
                </div>

                <div class="flex-1 py-4 rounded-lg inline-flex flex-col justify-center items-center gap-2.5 bg-[var(--background-300)] shadow-[10px_10px_22px_0px_rgba(255,255,255,0.25)] hover:shadow-[0_0_25px_rgba(255,255,255,0.80)] transition duration-150 ease-in-out overflow-hidden" @click="() => $router.push('/settings')">
                    <ProfileIcon class="h-6 invert opacity-65" />
                    <div class="text-center justify-center text-[var(--background-700)] text-base font-bold ">Profile</div>
                </div>
            </div>


            <div class="self-stretch h-[465px] pt-6 inline-flex flex-col justify-start items-start gap-3.5">
                <div class="text-center justify-center text-white text-2xl font-bold">Your Stats</div>

                <!-- Fast Stats Display -->
                <div class="self-stretch py-5 inline-flex justify-between items-center overflow-hidden">
                    <!-- Win Rate -->
                    <div class="size- inline-flex flex-col justify-center items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-white text-xs font-semibold">Win rate</div>
                        <div class="text-center justify-center text-[var(--background-200)] text-xl font-bold">
                            <span v-if="loading">...</span>
                            <span v-else>{{ stats.overallWinRate }}%</span>
                        </div>
                    </div>

                    <!-- Favourite Card -->
                    <div class="size- inline-flex flex-col justify-center items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-white text-xs font-semibold">Favourite Card</div>
                        <div class="text-center justify-center text-[var(--background-200)] text-xl font-bold">
                            <span v-if="loading">...</span>
                            <span v-else>Doom Slayer</span>
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
                            <span v-else>Player A</span>
                        </div>
                    </div>
                </div>










                <div class="self-stretch h-0 outline outline-offset-[-1px] outline-[var(--background-300)] rounded-full"></div>

                <div class="self-stretch inline-flex justify-between items-center">
                    <div class="text-center justify-center text-white text-lg font-bold">Your matches</div>
                    <div class="text-center justify-center text-white text-sm font-light">see more</div>
                </div>

                <div class="self-stretch flex-1 flex flex-col justify-start items-center gap-3.5 overflow-hidden">
                    <div class="self-stretch px-6 py-5 relative bg-Outer-Space-900 rounded-xl inline-flex justify-between items-center overflow-hidden">
                        <div class="size- flex justify-start items-center gap-6">
                            <div class="size-11 bg-Outer-Space-400"></div>
                            <div class="size- inline-flex flex-col justify-center items-start">
                                <div class="self-stretch justify-center text-white text-xl font-bold font-['Inter']">Against</div>
                                <div class="justify-center text-white text-lg font-normal font-['Inter']">Doom Slayer</div>
                            </div>
                        </div>
                        <div class="w-3 h-7 outline outline-[3px] outline-offset-[-1.50px] outline-Outer-Space-300"></div>
                        <div class="size- left-[427px] top-[27px] absolute flex justify-center items-center gap-2.5 overflow-hidden">
                            <div class="text-center justify-center text-emerald-400 text-3xl font-bold font-['Inter'] tracking-widest">12 - 7</div>
                        </div>
                    </div>
                    <div class="self-stretch px-6 py-5 relative bg-Outer-Space-900 rounded-xl inline-flex justify-between items-center overflow-hidden">
                        <div class="size- flex justify-start items-center gap-6">
                            <div class="size-11 bg-Outer-Space-400"></div>
                            <div class="size- inline-flex flex-col justify-center items-start">
                                <div class="self-stretch justify-center text-white text-xl font-bold font-['Inter']">Against</div>
                                <div class="justify-center text-white text-lg font-normal font-['Inter']">Doom Slayer</div>
                            </div>
                        </div>
                        <div class="w-3 h-7 outline outline-[3px] outline-offset-[-1.50px] outline-Outer-Space-300"></div>
                        <div class="size- left-[426px] top-[27px] absolute flex justify-center items-center gap-2.5 overflow-hidden">
                            <div class="text-center justify-center text-red-500 text-3xl font-bold font-['Inter'] tracking-widest">8 - 12</div>
                        </div>
                    </div>
                    <div class="self-stretch px-6 py-5 relative bg-Outer-Space-900 rounded-xl inline-flex justify-between items-center overflow-hidden">
                        <div class="size- flex justify-start items-center gap-6">
                            <div class="size-11 bg-Outer-Space-400"></div>
                            <div class="size- inline-flex flex-col justify-center items-start">
                                <div class="self-stretch justify-center text-white text-xl font-bold font-['Inter']">Against</div>
                                <div class="justify-center text-white text-lg font-normal font-['Inter']">Doom Slayer</div>
                            </div>
                        </div>
                        <div class="w-3 h-7 outline outline-[3px] outline-offset-[-1.50px] outline-Outer-Space-300"></div>
                        <div class="size- left-[426px] top-[27px] absolute flex justify-center items-center gap-2.5 overflow-hidden">
                            <div class="text-center justify-center text-red-500 text-3xl font-bold font-['Inter'] tracking-widest">8 - 12</div>
                        </div>
                    </div>
                </div>
            </div>


            <!-- Stats -->
            <div v-if="stats">
                <h1 class="text-3xl font-bold">Stats:</h1>
                <pre>{{ JSON.stringify(stats, null, 2) }}</pre>
                <pre>{{ stats.overallWinRate }}</pre>
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
