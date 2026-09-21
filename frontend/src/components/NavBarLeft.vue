<script setup>
import HomeIcon from '../assets/HomeIcon.svg?component'
import PlayIcon from '../assets/PlayIcon.svg?component'
import StatIcon from '../assets/StatIcon.svg?component'
import DeckIcon from '../assets/DeckIcon.svg?component'
import ParamIcon from '../assets/ParamIcon.svg?component'
import ProfileIcon from '../assets/ProfileIcon.svg?component'

import tempIcon from '../assets/vue.svg?component'

import NavbarButton from '../components/NavbarButton.vue'



defineProps({
    loading: {
        type: Boolean,
        required: true
    },

    username: {
        type: String,
        required: true
    }
})

</script>


<template>
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
            <div class="flex flex-col">
                <NavbarButton text="Home" :icon="HomeIcon" :disabled="loading" :active="$route.path === '/'" @click="() => $router.push('/')" />
                <NavbarButton text="Play" :icon="PlayIcon" :disabled="loading" :active="$route.path === '/play'" @click="() => $router.push('/play')" />
                <NavbarButton text="Dashboard" :icon="StatIcon" :disabled="loading" :active="$route.path === '/dashboard'" @click="() => $router.push('/dashboard')" />
                <NavbarButton text="Decks" :icon="DeckIcon" :disabled="loading" :active="$route.path === '/decks'" @click="() => $router.push('/decks')" />
                <NavbarButton text="Settings" :icon="ParamIcon" :disabled="loading" :active="$route.path === '/settings'" @click="() => $router.push('/settings')" />
            </div>
        </div>

        <!-- Lower Section -->
        <div class="flex px-[8px] items-center gap-[18px] self-stretch">
            <ProfileIcon class="w-[50px] h-[50px]" />
            <!-- Line -->
            <div class="w-[2px] rounded-md h-full bg-white"></div>

            <!-- Level Section -->
            <div class="flex flex-col flex-1 gap-[5px] items-start">
                <h3 class="w-full text-white text-[14px] text-left leading-tight font-[var(--Bold)] overflow-x-auto whitespace-nowrap no-scrollbar">{{ username }}</h3>

                <div class="flex flex-col w-full items-start">
                    <p class="text-white text-[12px] font-[var(--Regular)]">Level 12</p>
                    <!-- Progression Bar -->
                    <div class="relative w-full">
                        <div class="h-[4px] rounded-md w-full bg-white opacity-50"></div>
                        <div class="absolute top-0 left-0 h-[4px] rounded-md w-[60%] bg-white"></div>
                    </div>
                </div>
            </div>
        </div>
    </nav>
</template>