<script setup>
import { ref, reactive } from 'vue'

const active = ref('you')

const you = reactive({ name: '', deck: '' })
const opponent = reactive({ name: '', deck: '' })

const scoreYou = ref(0)
const scoreOpponent = ref(0)
</script>


<template>
    <div class="relative w-full h-screen overflow-hidden bg-slate-900">

        <!-- Tab de rappel "You" -->
        <button @click="active = 'you'" class="absolute left-0 top-1/2 -translate-y-1/2 z-30 bg-[var(--background-400)] text-black font-bold italic px-2 py-6 rounded-r-lg shadow-lg">
            You
        </button>


        <!-- Panneau "You" -->
        <div @click="active = 'you'" class="absolute top-0 left-0 h-full w-[80%] bg-[var(--background-400)] p-6 flex flex-col transition-transform duration-500 ease-in-out z-20" :class="active === 'you' ? 'translate-x-0' : '-translate-x-[75%]'">
            <div class="self-stretch pb-16 inline-flex justify-end items-start gap-2.5">
                <div class="text-center justify-center text-black text-4xl font-bold italic">You</div>
            </div>


            <div class="flex flex-col gap-8">
                <div class="self-stretch inline-flex flex-col justify-center items-start gap-3.5">
                    <div class="self-stretch p-4 bg-[var(--background-300)] rounded-lg inline-flex justify-start items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-[var(--background-600)] text-sm font-medium">select the <span class="font-bold">your name</span></div>
                    </div>
                </div>

                <div class="self-stretch inline-flex flex-col justify-center items-start gap-2">
                    <div class="text-center justify-center text-black text-xl font-semibold italic">Deck</div>
                    <div class="self-stretch p-4 bg-[var(--background-300)] rounded-lg inline-flex justify-start items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-[var(--background-600)] text-sm font-medium ">select the <span class="font-bold">your Deck</span></div>
                    </div>
                </div>
            </div>

        </div>



        <!-- Tab de rappel "Opponent" -->
        <button @click="active = 'opponent'" class="absolute right-0 top-1/2 -translate-y-1/2 z-30 bg-[var(--background-900)] text-white font-bold italic px-2 py-6 rounded-l-lg shadow-[0_0px_12px_rgba(255,255,255,0.2)] ">
            Opponent
        </button>

        <!-- Panneau "Opponent" -->
        <div @click="active = 'opponent'" class="absolute top-0 right-0 h-full w-[80%] bg-[var(--background-900)] p-6 flex flex-col transition-transform duration-500 ease-in-out z-20" :class="active === 'opponent' ? 'translate-x-0' : 'translate-x-[75%]'">
            <div class="self-stretch pb-16 inline-flex justify-start items-start gap-2.5">
                <div class="text-center justify-center text-white text-4xl font-bold italic">Opponent</div>
            </div>

            <div class="flex flex-col gap-8">
                <div class="self-stretch inline-flex flex-col justify-center items-start gap-3.5">
                    <div class="self-stretch p-4 bg-[var(--background-600)] rounded-lg inline-flex justify-start items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-[var(--background-400)] text-sm font-medium">select the <span class="font-bold">opponent name</span></div>
                    </div>
                </div>

                <div class="self-stretch inline-flex flex-col justify-center items-start gap-2">
                    <div class="text-center justify-center text-white text-xl font-semibold italic">Deck</div>
                    <div class="self-stretch p-4 bg-[var(--background-600)] rounded-lg inline-flex justify-start items-center gap-2.5 overflow-hidden">
                        <div class="text-center justify-center text-[var(--background-400)] text-sm font-medium ">select the <span class="font-bold">opponent Deck</span></div>
                    </div>
                </div>
            </div>

        </div>









        <!-- Score : indépendant des panneaux, toujours centré en bas, blocs collés -->
        <div class="absolute bottom-8 left-1/2 -translate-x-1/2 z-40 flex items-stretch">
            <div class="flex flex-col items-center gap-2 px-6">
                <button @click="scoreYou++" class="text-white/70 hover:text-white">▲</button>
                <span class="text-5xl font-extrabold text-white">{{ scoreYou }}</span>
                <button @click="scoreYou > 0 && scoreYou--" class="text-white/70 hover:text-white">▼</button>
            </div>

            <div class="flex items-center text-4xl font-extrabold text-white px-2">-</div>

            <div class="flex flex-col items-center gap-2 px-6">
                <button @click="scoreOpponent++" class="text-white/70 hover:text-white">▲</button>
                <span class="text-5xl font-extrabold text-white">{{ scoreOpponent }}</span>
                <button @click="scoreOpponent > 0 && scoreOpponent--" class="text-white/70 hover:text-white">▼</button>
            </div>
        </div>

    </div>
</template>
