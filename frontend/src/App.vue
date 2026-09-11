<script setup>
import { ref, onMounted } from 'vue'
import HomeIcon from './assets/HomeIcon.svg?component'
import PlayIcon from './assets/PlayIcon.svg?component'
import StatIcon from './assets/StatIcon.svg?component'
import DeckIcon from './assets/DeckIcon.svg?component'
import ParamIcon from './assets/ParamIcon.svg?component'
import ProfileIcon from './assets/ProfilIcon.svg?component'

import tempIcon from './assets/vue.svg?component'

import NavbarButton from './components/NavbarButton.vue'

const stats = ref(null)
const matches = ref([])
const loading = ref(true)
const legends = ref([])
const error = ref(null)

const username = ref('Free Player')

async function load() {
  try {
    // STATS
    const statsResponse = await fetch('/api/stats')
    if (!statsResponse.ok) {
      throw new Error('Loading Stats failed')
    }
    stats.value = await statsResponse.json()

    // MATCHES
    const matchesResponse = await fetch('/api/matches')
    if (!matchesResponse.ok) {
      throw new Error('Loading Matches failed')
    }
    matches.value = await matchesResponse.json()

    // LEGENDS
    const legendsResponse = await fetch('/api/legends')
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

    <!-- Left Navigation -->
    <nav class="flex flex-col w-[25%] h-screen bg-[var(--bg-navbar)] px-[20px] py-[40px]">
      <!-- Upper Section -->
      <div class="flex flex-col gap-[35px] flex-1">

        <!-- Upper Titles -->
        <div class="flex flex-col gap-[15px] justify-center items-center">
          <tempIcon class="w-[50px] h-[50px]" />
          <h2 class="text-white text-[38px] font-[var(--SemiBold)] tracking-[12%]">CardQuest</h2>
        </div>

        <!-- Navigation Buttons -->
        <div class="flex flex-col"> <!-- @click="() => $router.push('/stats')"-->
          <NavbarButton text="Home" :icon="HomeIcon" :disabled="loading" />
          <NavbarButton text="Play" :icon="PlayIcon" :disabled="loading" />
          <NavbarButton text="Stats" :icon="StatIcon" :disabled="loading" />
          <NavbarButton text="Decks" :icon="DeckIcon" :disabled="loading" />
          <NavbarButton text="Settings" :icon="ParamIcon" :disabled="loading" />
        </div>
      </div>

      <!-- Lower Section -->
      <div class="flex px-[20px] py-[5px] items-center gap-[18px] self-stretch">
        <ProfileIcon class="w-[50px] h-[50px]" />
        <!-- Line -->
        <div class="w-[2px] rounded-md h-full bg-white"></div>

        <!-- Level Section -->
        <div class="flex flex-col flex-1 gap-[5px] items-start">
          <h3 class="text-white text-[14px] font-[var(--Bold)]">{{ username }}</h3>

          <div class="flex flex-col w-full items-start">
            <p class="text-white text-[12px] font-[var(--Regular)]">Level 12</p>
            <!-- Progression Bar -->
            <div class="relative w-full">
              <div class="h-[3px] rounded-md w-full bg-white opacity-50"></div>
              <div class="absolute top-0 left-0 h-[3px] rounded-md w-[60%] bg-white"></div>
            </div>
          </div>
        </div>
      </div>
    </nav>

  </main>
</template>
