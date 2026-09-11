<script setup>
import { ref, onMounted } from 'vue'

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
    <h1>Riftlog — manual check</h1>

    <p>
      Minimal debug page: shows exactly what the API returns, nothing more.
    </p>

    <h2>Legends</h2>

    <pre v-if="loading">loading...</pre>
    <pre v-else-if="error">{{ error }}</pre>
    <p v-else v-for="legend in legends" :key="legend.id">{{ legend.name }}</p>

    <h2>Matches</h2>

    <table>
      <thead>
        <tr>
          <th>Date</th>
          <th>Opponent</th>
          <th>My deck</th>
          <th>Their deck</th>
          <th>Result</th>
          <th>Score</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="loading">
          <td colspan="6">loading...</td>
        </tr>

        <tr v-else-if="error">
          <td colspan="6">{{ error }}</td>
        </tr>

        <tr v-else-if="matches.length === 0">
          <td colspan="6">No matches logged yet</td>
        </tr>

        <tr v-else v-for="match in matches" :key="match.id">
          <td>{{ new Date(match.playedAt).toLocaleString() }}</td>
          <td>{{ match.opponentName }}</td>
          <td>{{ match.myDeckName }}</td>
          <td>{{ match.opponentDeckName }}</td>

          <td :class="match.result === 'WIN' ? 'win' : 'loss'">
            {{ match.result }}
          </td>

          <td>
            {{ match.myFinalScore }} - {{ match.opponentFinalScore }}
          </td>
        </tr>
      </tbody>
    </table>
  </main>
</template>

<style scoped>
main {
  font-family: sans-serif;
  max-width: 700px;
  margin: 2rem auto;
  padding: 0 1rem;
}

h1 {
  font-size: 1.4rem;
}

h2 {
  font-size: 1.1rem;
  margin-top: 2rem;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 0.5rem;
}

th,
td {
  text-align: left;
  padding: 0.4rem;
  border-bottom: 1px solid #ddd;
}

.win {
  color: green;
}

.loss {
  color: crimson;
}
</style>