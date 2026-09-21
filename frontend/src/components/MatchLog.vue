<script setup>
import SwordIcon from '../assets/SwordIcon.svg?component'
import GotoIcon from '../assets/GoToIcon.svg?component'


defineProps({
    opponent: {
        type: String,
        default: "N/A"
    },

    result: {
        type: Boolean,
        daulft: false
    },

    score: {
        type: String,
        default: "N/A"
    },

    deck: {
        type: String,
        default: 'N/A'
    },

    date: {
        type: Date,
        default: 'N/A',
    }

})


function formatDate(date) {
    return new Date(date).toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
    })
}

</script>



<template>
    <div class="self-stretch flex-1 flex flex-col justify-start items-center gap-3.5 overflow-hidden bg-[var(--background-900)] rounded-xl hover:scale-102 transition duration-300 ease-in-out">
        <div class="self-stretch px-6 py-5 relative bg-Outer-Space-900 rounded-xl inline-flex justify-between items-center overflow-hidden">


            <div class="size- flex justify-start items-center gap-6">
                <SwordIcon class="h-9" />

                <div class="size- inline-flex flex-col justify-center items-start">
                    <div class="justify-start text-white text-xl font-bold ">Against</div>
                    <div class="justify-start text-white text-lg font-normal ">{{ opponent }}</div>
                </div>
            </div>

            <GotoIcon class="h-8" />

            <div class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 flex justify-center items-center gap-2.5">
                <div class="flex flex-col">
                    <div class="text-center justify-center text-3xl font-bold tracking-widest" :class="result ? 'text-emerald-400' : 'text-[#CC4547]'">
                        {{ score }}
                    </div>
                    <div class="flex flex-row gap-2 items-center justify-center leading-none">
                        <span class="text-[10px]">deck :</span>
                        <span class="text-xs"> {{ deck }}</span>
                    </div>
                    {{ formatDate(date) }}

                </div>


            </div>
        </div>
    </div>
</template>