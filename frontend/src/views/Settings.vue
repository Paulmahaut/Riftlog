<script setup>
import { ref, computed, onMounted } from 'vue'
import { apiFetch } from '../api.js'
import NavBarLeft from '../components/NavBarLeft.vue'

import ProfileIcon from "../assets/ProfileIcon.svg?component"

const loading = ref(true)
const error = ref(null)

const username = ref('')
const stats = ref(null)
const matches = ref([])



async function load() {
    try {
        // USERNAME
        const response = await apiFetch('/api/auth/me')

        if (!response.ok) {
            throw new Error('Impossible de récupérer le username')
        }
        const user = await response.json()

        username.value = user?.displayName ?? 'N/A'

        // STATS
        const statsResponse = await apiFetch('/api/stats')

        if (!statsResponse.ok) {
            throw new Error('Loading Stats failed')
        }
        stats.value = await statsResponse.json()

        // MATCHES
        const matchesResponse = await apiFetch('/api/matches')
        if (!matchesResponse.ok) {
            throw new Error('Loading Matches failed')
        }
        matches.value = await matchesResponse.json()


    } catch (e) {
        error.value = e.message
    } finally {
        loading.value = false
    }
}

onMounted(load)


const totalWins = computed(() => {
    return matches.value.filter(match => match.result === 'WIN').length
})

</script>





<template>
    <main class="flex flex-row align-items-center bg-[var(--background)]">

        <NavBarLeft :loading="loading" :username="username" />

        <!-- Main Test -->
        <div class="flex flex-col gap-10 w-full max-h-screen p-8 overflow-y-auto bg-[var(--bg)]">

            <!-- Profile + Level bar Display -->
            <div class="self-stretch inline-flex justify-start gap-9">
                <ProfileIcon class="h-20" />

                <div class="flex-1 self-stretch inline-flex flex-col justify-center items-start gap-[5px] overflow-hidden">
                    <div class="text-center justify-center text-white text-2xl font-semibold tracking-widest">{{ username }}</div>

                    <div class="self-stretch relative flex flex-col justify-start items-start gap-1.5">
                        <div class="self-stretch inline-flex justify-between items-center">
                            <div class="text-center justify-center text-[var(--background-300)] text-base font-normal">Level 12</div>
                            <div class="text-center justify-center text-[var(--background-600)] text-xs font-normal ">860 / 1230 XP</div>
                        </div>

                        <!-- Progression Bar -->
                        <div class="relative self-stretch h-1 bg-white/50 rounded-full overflow-hidden">
                            <div class="absolute left-0 top-0 h-full w-[60%] bg-white rounded-full"></div>
                        </div>
                    </div>
                </div>

                <!-- Missing XP -->
                <div class="self-stretch py-[5px] inline-flex flex-col justify-end items-start gap-2.5 overflow-hidden">
                    <div class="text-center justify-center text-[var(--background-600)] text-[12px] font-normal ">avg XP / match : 186.8</div>
                </div>
            </div>

            <!-- Separetor -->
            <div class="self-stretch h-1 bg-[var(--background-600)] rounded-full"></div>

            <!-- Fast Stats Dislpay -->
            <div class="self-stretch inline-flex justify-start items-center gap-4 overflow-hidden">
                <div class="flex-1 px-4 py-4 bg-white/10 backdrop-blur-[15px] backdrop-saturate-[100%] border border-white/30 rounded-[8px] shadow-[0_8px_32px_0_rgba(0,0,0,0)] rounded-lg inline-flex flex-col justify-start items-start gap-3.5 overflow-hidden ">
                    <div class="text-center justify-center text-white text-xl font-bold ">Matches Played</div>
                    <div class="text-center justify-center text-[var(--background-400)] text-3xl font-normal ">
                        <span v-if="loading">...</span>
                        <span v-else>{{ stats.totalMatches }}</span>
                    </div>
                </div>

                <div class="flex-1 px-4 py-4 bg-white/10 backdrop-blur-[15px] backdrop-saturate-[100%] border border-white/30 rounded-[8px] shadow-[0_8px_32px_0_rgba(0,0,0,0)] rounded-lg inline-flex flex-col justify-start items-start gap-3.5 overflow-hidden">
                    <div class="text-center justify-center text-white text-xl font-bold ">Victory</div>
                    <div class="text-center justify-center text-[var(--background-400)] text-3xl font-normal ">
                        <span v-if="loading">...</span>
                        <span v-else>{{ totalWins }}</span>
                    </div>
                </div>

                <div class="flex-1 px-4 py-4 bg-white/10 backdrop-blur-[15px] backdrop-saturate-[100%] border border-white/30 rounded-[8px] shadow-[0_8px_32px_0_rgba(0,0,0,0)]0 rounded-lg inline-flex flex-col justify-start items-start gap-3.5 overflow-hidden">
                    <div class="text-center justify-center text-white text-xl font-bold ">Avg. Match time</div>
                    <div class="text-center justify-center text-[var(--background-400)] text-3xl font-normal ">23”42</div>
                </div>
            </div>


            <div class="size-lf-stretch py-4 inline-flex flex-col justify-start items-start gap-4">
                <div class="text-center justify-center text-white text-2xl font-bold tracking-wide">Recent Deck used</div>

                <div class="self-stretch flex-1 flex flex-col justify-start items-center gap-3.5">
                    <div class="self-stretch px-6 py-8 relative bg-Outer-Space-900 rounded-xl inline-flex justify-between items-center">
                        <div class="size- flex justify-start items-center gap-6">
                            <div class="size-11 bg-Outer-Space-400"></div>
                            <div class="size- inline-flex flex-col justify-center items-start">
                                <div class="justify-center text-white text-xl font-bold font-['Inter']">Pyro Deck</div>
                                <div class="justify-center text-white text-lg font-normal font-['Inter']">used 24 times</div>
                            </div>
                        </div>
                        <div class="w-3 h-7 outline outline-[3px] outline-offset-[-1.50px] outline-Outer-Space-300"></div>
                        <div class="size- left-[413.50px] top-[26px] absolute inline-flex flex-col justify-center items-center overflow-hidden">
                            <div class="text-center justify-center text-Outer-Space-200 text-3xl font-bold font-['Inter'] tracking-widest">win rate</div>
                            <div class="text-center justify-center text-Outer-Space-400 text-xl font-bold font-['Inter']">52 %</div>
                        </div>
                    </div>

                </div>
            </div>


        </div>


    </main>
</template>
