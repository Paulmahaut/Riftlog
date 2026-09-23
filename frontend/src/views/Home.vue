<script setup>
import { ref, onMounted } from 'vue'
import { apiFetch } from '../api.js'
import NavBarLeft from '../components/NavBarLeft.vue'

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
        <div class="flex flex-col gap-16 w-full max-h-screen p-8 overflow-y-auto bg-[var(--bg)]">
            <div class="self-stretch flex flex-col justify-start items-center gap-3">
                <div class="size-35 bg-zinc-300 rounded-full"></div>
                <div class="self-stretch text-center justify-center text-white text-6xl font-extrabold">CardQuest</div>
            </div>

            <div class="w-[50%] h-fit flex flex-col justify-center items-center gap-10 m-auto">
                <div class="self-stretch h-14 bg-[var(--background-200)] rounded-lg flex flex-col justify-center items-center gap-2.5 overflow-hidden hover:cursor-pointer" @click="() => $router.push('/play')">
                    <div class="text-center justify-center text-[var(--background-900)] text-base font-extrabold">Play</div>
                </div>

                <div class="self-stretch h-14 rounded-lg outline outline-[3px] outline-offset-[-3px] outline-[var(--background-200)] flex flex-col justify-center items-center gap-2.5 overflow-hidden hover:cursor-pointer" @click="() => $router.push('/decks')">
                    <div class="text-center justify-center text-[var(--background-200)] text-base font-bold ">My Decks</div>
                </div>
            </div>


        </div>


    </main>
</template>
