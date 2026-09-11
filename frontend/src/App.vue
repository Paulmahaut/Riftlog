<script setup>
import { ref, onMounted } from 'vue'
import HomeIcon from './assets/HomeIcon.svg?component'
import PlayIcon from './assets/PlayIcon.svg?component'
import StatIcon from './assets/StatIcon.svg?component'
import DeckIcon from './assets/DeckIcon.svg?component'
import ParamIcon from './assets/ParamIcon.svg?component'

import NavbarButton from './components/NavbarButton.vue'

const stats = ref(null)
const matches = ref([])
const loading = ref(true)
const legends = ref([])
const error = ref(null)

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
  <main>
    <nav class="LeftNav">
      <!-- @click="() => $router.push('/stats')"-->
      <NavbarButton text="Home" :icon="HomeIcon" :disabled="loading" />
      <NavbarButton text="Play" :icon="PlayIcon" :disabled="loading" />
      <NavbarButton text="Stats" :icon="StatIcon" :disabled="loading" />
      <NavbarButton text="Decks" :icon="DeckIcon" :disabled="loading" />
      <NavbarButton text="Settings" :icon="ParamIcon" :disabled="loading" />
    </nav>

  </main>
</template>





<style scoped>
main {
  display: flex;
  flex-direction: row;
  align-items: center;
  background-color: var(--background);
}

.LeftNav {
  width: 25%;
  height: 100vh;
  background-color: var(--bg-navbar);
}
</style>