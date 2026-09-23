<script setup>
import { ref, onMounted } from 'vue'
import { apiFetch } from '../api.js'
import NavBarLeft from '../components/NavBarLeft.vue'
import Test from '../components/test.vue'

const loading = ref(true)
const error = ref(null)

const username = ref('')

async function load() {
    try {
        // USERNAME
        const response = await apiFetch('/api/auth/me')

        if (!response.ok) {
            throw new Error('Impossible de récupérer le username')
        }
        const user = await response.json()

        username.value = user?.displayName ?? 'N/A'

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
        <div class="flex flex-col gap-16 w-full max-h-screen overflow-y-auto bg-[var(--bg)]">

            <Test />


        </div>


    </main>
</template>
