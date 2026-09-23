<script setup>
import DeckCard from '../assets/DeckCard.svg?component'
import GoToIcon from '../assets/GoToIcon.svg?component'



defineProps({
    deckName: {
        type: String,
        default: "N/A"
    },

    usage: {
        type: Number,
        default: 0
    },

    winRate: {
        type: Number,
        default: 0
    }
})

</script>


<template>

    <div class="self-stretch px-8 py-6 relative bg-[var(--background-900)] rounded-xl inline-flex justify-between items-center">
        <div class="size- flex justify-start items-center gap-6">
            <DeckCard class="h-10" />

            <div class="size- inline-flex flex-col justify-center items-start">
                <div class="justify-center text-white text-lg font-bold ">{{ deckName }}</div>
                <div class="justify-center text-white text-sm font-normal ">used {{ usage }} times</div>
            </div>
        </div>
        <GoToIcon class="h-6" />

        <div class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 inline-flex flex-col justify-center items-center overflow-hidden">
            <div class="text-center justify-center text-[var(--background-200)] text-2xl font-bold tracking-widest">win rate</div>
            <div class="text-center justify-center text-[var(--background-400)] text-base font-bold">{{ winRate }} %</div>
        </div>
    </div>
</template>
