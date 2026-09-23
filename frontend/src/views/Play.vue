<script setup>
import { ref, onMounted, reactive } from 'vue'
import { apiFetch } from '../api.js'
import NavBarLeft from '../components/NavBarLeft.vue'
import addButton from '../assets/addButton.svg?component'

const active = ref('you')

const you = reactive({
    name: '',
    deck: '',
    legend: ''
})

const opponent = reactive({
    name: '',
    deck: '',
    legend: ''
})

const scoreYou = ref(0)
const scoreOpponent = ref(0)

const loading = ref(true)
const error = ref(null)

const username = ref('')
const matches = ref([])
const decks = ref([])
const legends = ref([])

// le Dropdown actuellement ouvert
const openDropdown = ref(null)

async function load() {
    try {
        // USERNAME
        const response = await apiFetch('/api/auth/me')

        if (!response.ok) {
            throw new Error('Impossible de récupérer le username')
        }

        const user = await response.json()
        username.value = user?.displayName ?? 'N/A'

        // MATCHES
        const matchesResponse = await apiFetch('/api/matches')

        if (!matchesResponse.ok) {
            throw new Error('Loading Matches failed')
        }

        matches.value = await matchesResponse.json()

        // DECKS
        const decksResponse = await apiFetch('/api/decks')

        if (!decksResponse.ok) {
            throw new Error('Loading Decks failed')
        }

        decks.value = await decksResponse.json()

        // LEGENDS
        const legendsResponse = await apiFetch('/api/legends')

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


// Joueurs rencontrés précédemment
const opponents = () => {
    return [...new Set(
        matches.value
            .map(match => match.opponentName)
            .filter(Boolean)
    )]
}


// Ouvre - ferme un dropdown
function toggleDropdown(name) {
    openDropdown.value =
        openDropdown.value === name
            ? null
            : name
}


// Enregistre un choix
function selectValue(object, property, value) {
    object[property] = value
    openDropdown.value = null
}


// Ajouter le match
async function addMatch() {

    error.value = null

    if (
        !you.name ||
        !you.deck ||
        !you.legend ||
        !opponent.name ||
        !opponent.deck ||
        !opponent.legend
    ) {
        error.value = 'Veuillez remplir toutes les sélections'
        return
    }

    if (scoreYou.value !== 8 && scoreOpponent.value !== 8) {
        error.value = 'Le match doit avoir un score final de 8'
        return
    }

    try {
        error.value = null

        const request = {
            opponentName: opponent.name,
            myLegendName: you.legend,
            myDeckName: you.deck,
            opponentLegendName: opponent.legend,
            opponentDeckName: opponent.deck,

            rounds: [
                {
                    roundNumber: 1,
                    myScore: scoreYou.value,
                    opponentScore: scoreOpponent.value
                }
            ]
        }

        const response = await apiFetch('/api/matches', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        })

        if (!response.ok) {
            const message = await response.text()
            throw new Error(message || 'Impossible d’ajouter le match')
        }

        // Match ajouté
        const savedMatch = await response.json()

        matches.value.unshift(savedMatch)

        // Reset
        you.name = ''
        you.deck = ''
        you.legend = ''

        opponent.name = ''
        opponent.deck = ''
        opponent.legend = ''

        scoreYou.value = 0
        scoreOpponent.value = 0

        openDropdown.value = null

    } catch (e) {
        error.value = e.message
    }
}


function filteredDecks(search) {
    const value = search.toLowerCase().trim()

    const filtered = !value
        ? decks.value
        : decks.value.filter(deck =>
            deck.name.toLowerCase().includes(value)
        )

    return [...new Map(
        filtered.map(deck => [deck.name.toLowerCase(), deck])
    ).values()]
}

function filteredLegends(search) {
    const value = search.toLowerCase().trim()

    if (!value) {
        return legends.value
    }

    return legends.value.filter(legend =>
        legend.name.toLowerCase().includes(value)
    )
}

function filteredOpponents(search) {
    const value = search.toLowerCase().trim()

    const names = opponents()

    if (!value) {
        return names
    }

    return names.filter(name =>
        name.toLowerCase().includes(value)
    )
}

/*
- ne pas utiliser les opponents des matchs précédent, mais directement les "Users" + supprimer la table player qui ne sert à rien
- faire une page register + page login
- améliorer la beauté de la page 'Play.vue'
- 
*/
</script>





<template>
    <main class="flex flex-row align-items-center bg-[var(--background)]">

        <NavBarLeft :loading="loading" :username="username" />

        <!-- Main Test -->
        <div class="flex flex-col gap-16 w-full max-h-screen overflow-y-auto bg-[var(--bg)]">

            <div class="relative w-full h-screen overflow-hidden bg-slate-900">
                <!-- Tab de rappel "You" -->
                <button @click="active = 'you'" class="absolute left-0 top-3/4 -translate-y-3/4 z-30 bg-[var(--background-400)] text-black font-bold italic px-2 py-6 rounded-r-lg shadow-lg">
                    You
                </button>


                <!-- Panneau "You" -->
                <div @click="active = 'you'" class="absolute top-0 left-0 h-full w-[80%] bg-[var(--background-400)] p-6 flex flex-col transition-transform duration-500 ease-in-out z-20" :class="active === 'you' ? 'translate-x-0' : '-translate-x-[75%]'">
                    <div class="self-stretch pb-16 inline-flex justify-end items-start gap-2.5">
                        <div class="text-center justify-center text-black text-4xl font-bold italic">You</div>
                    </div>


                    <div class="flex flex-col gap-8">

                        <!-- You Name -->
                        <div class="relative self-stretch">

                            <div @click.stop="toggleDropdown('youName')" class="self-stretch p-4 bg-[var(--background-300)] rounded-lg flex justify-between items-center cursor-pointer hover:bg-[var(--background-200)] transition">
                                <div class="text-sm font-medium text-[var(--background-600)]">
                                    {{ you.name || 'select your name' }}
                                </div>

                                <span>▼</span>
                            </div>

                            <div v-if="openDropdown === 'youName'" class="absolute left-0 right-0 top-full mt-2 z-50 bg-[var(--background-200)] rounded-lg overflow-hidden shadow-xl">
                                <div @click="selectValue(you, 'name', username)" class="p-4 cursor-pointer hover:bg-[var(--background-300)]">
                                    {{ username }}
                                </div>
                            </div>

                        </div>

                        <!-- You Deck -->
                        <div class="relative">
                            <div class="text-left justify-center pb-1 text-black text-xl font-semibold italic">Deck</div>

                            <input v-model="you.deck" @focus="openDropdown = 'youDeck'" @click.stop type="text" placeholder="select your Deck" class="w-full p-4 bg-[var(--background-300)] rounded-lg outline-none text-[var(--background-600)] text-sm font-medium placeholder:text-[var(--background-600)]" />

                            <div v-if="openDropdown === 'youDeck'" class="absolute left-0 right-0 top-full mt-2 z-50 bg-[var(--background-200)] rounded-lg overflow-hidden shadow-xl max-h-48 overflow-y-auto">
                                <div v-for="deck in filteredDecks(you.deck)" :key="deck.id" @click="selectValue(you, 'deck', deck.name)" class="p-4 cursor-pointer hover:bg-[var(--background-300)]">
                                    {{ deck.name }}
                                </div>

                                <div v-if="filteredDecks(you.deck).length === 0" class="p-4 text-[var(--background-600)]">
                                    Aucun deck trouvé
                                </div>
                            </div>

                        </div>


                        <!-- You Legend -->
                        <div class="relative">
                            <div class="text-left justify-center pb-1 text-black text-xl font-semibold italic">Legend</div>

                            <input v-model="you.legend" @focus="openDropdown = 'youLegend'" @click.stop type="text" placeholder="select your Legend" class="w-full p-4 bg-[var(--background-300)] rounded-lg outline-none text-[var(--background-600)] text-sm font-medium placeholder:text-[var(--background-600)]" />

                            <div v-if="openDropdown === 'youLegend'" class="absolute left-0 right-0 top-full mt-2 z-50 bg-[var(--background-200)] rounded-lg overflow-hidden shadow-xl max-h-48 overflow-y-auto">
                                <div v-for="legend in filteredLegends(you.legend)" :key="legend.id" @click="selectValue(you, 'legend', legend.name)" class="p-4 cursor-pointer hover:bg-[var(--background-300)]">
                                    {{ legend.name }}
                                </div>

                                <div v-if="filteredLegends(you.legend).length === 0" class="p-4 text-[var(--background-600)]">
                                    Aucune légende trouvée
                                </div>
                            </div>

                        </div>
                    </div>

                </div>



                <!-- Tab de rappel "Opponent" -->
                <button @click="active = 'opponent'" class="absolute right-0 top-3/4 -translate-y-3/4 z-30 bg-[var(--background-900)] text-white font-bold italic px-2 py-6 rounded-l-lg shadow-[0_0px_12px_rgba(255,255,255,0.2)] ">
                    Opponent
                </button>

                <!-- Panneau "Opponent" -->
                <div @click="active = 'opponent'" class="absolute top-0 right-0 h-full w-[80%] bg-[var(--background-900)] p-6 flex flex-col transition-transform duration-500 ease-in-out z-20" :class="active === 'opponent' ? 'translate-x-0' : 'translate-x-[75%]'">
                    <div class="self-stretch pb-16 inline-flex justify-start items-start gap-2.5">
                        <div class="text-center justify-center text-white text-4xl font-bold italic">Opponent</div>
                    </div>

                    <div class="flex flex-col gap-8">
                        <!-- Opponent Name -->
                        <div class="relative">

                            <input v-model="opponent.name" @focus="openDropdown = 'opponentName'" @click.stop type="text" placeholder="select opponent name" class="w-full p-4 bg-[var(--background-600)] rounded-lg outline-none text-[var(--background-400)] text-sm font-medium placeholder:text-[var(--background-400)]" />

                            <div v-if="openDropdown === 'opponentName'" class="absolute left-0 right-0 top-full mt-2 z-50 bg-[var(--background-800)] rounded-lg overflow-hidden shadow-xl max-h-48 overflow-y-auto">
                                <div v-for="name in filteredOpponents(opponent.name)" :key="name" @click="selectValue(opponent, 'name', name)" class="p-4 text-white cursor-pointer hover:bg-[var(--background-700)]">
                                    {{ name }}
                                </div>

                                <div v-if="filteredOpponents(opponent.name).length === 0" class="p-4 text-white/50">
                                    Aucun joueur trouvé
                                </div>
                            </div>

                        </div>

                        <!-- Opponent Deck -->
                        <div class="relative">
                            <div class="text-left justify-center pb-1 text-white text-xl font-semibold italic">Deck</div>
                            <input v-model="opponent.deck" @focus="openDropdown = 'opponentDeck'" @click.stop type="text" placeholder="select opponent Deck" class="w-full p-4 bg-[var(--background-600)] rounded-lg outline-none text-[var(--background-400)] text-sm font-medium placeholder:text-[var(--background-400)]" />

                            <div v-if="openDropdown === 'opponentDeck'" class="absolute left-0 right-0 top-full mt-2 z-50 bg-[var(--background-800)] rounded-lg overflow-hidden shadow-xl max-h-48 overflow-y-auto">
                                <div v-for="deck in filteredDecks(opponent.deck)" :key="deck.id" @click="selectValue(opponent, 'deck', deck.name)" class="p-4 text-white cursor-pointer hover:bg-[var(--background-700)]">
                                    {{ deck.name }}
                                </div>

                                <div v-if="filteredDecks(opponent.deck).length === 0" class="p-4 text-white/50">
                                    Aucun deck trouvé
                                </div>
                            </div>

                        </div>

                        <!-- Oppoent Legend -->
                        <div class="relative">
                            <div class="text-left justify-center pb-1 text-white text-xl font-semibold italic">Legend</div>
                            <input v-model="opponent.legend" @focus="openDropdown = 'opponentLegend'" @click.stop type="text" placeholder="select opponent Legend" class="w-full p-4 bg-[var(--background-600)] rounded-lg outline-none text-[var(--background-400)] text-sm font-medium placeholder:text-[var(--background-400)]" />

                            <div v-if="openDropdown === 'opponentLegend'" class="absolute left-0 right-0 top-full mt-2 z-50 bg-[var(--background-800)] rounded-lg overflow-hidden shadow-xl max-h-48 overflow-y-auto">
                                <div v-for="legend in filteredLegends(opponent.legend)" :key="legend.id" @click="selectValue(opponent, 'legend', legend.name)" class="p-4 text-white cursor-pointer hover:bg-[var(--background-700)]">
                                    {{ legend.name }}
                                </div>

                                <div v-if="filteredLegends(opponent.legend).length === 0" class="p-4 text-white/50">
                                    Aucune légende trouvée
                                </div>
                            </div>

                        </div>
                    </div>

                </div>



                <!-- Score : indépendant des panneaux, toujours centré en bas, blocs collés -->
                <div class="absolute bottom-8 left-1/2 -translate-x-1/2 z-40 flex items-stretch">
                    <div class="flex flex-col items-center gap-2 px-6">
                        <button @click="scoreYou < 8 && scoreYou++" class="text-white/70 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed" :disabled="scoreYou >= 8">▲</button>
                        <span class="text-5xl font-extrabold text-white">{{ scoreYou }}</span>
                        <button @click="scoreYou > 0 && scoreYou--" class="text-white/70 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed" :disabled="scoreYou == 0">▼</button>
                    </div>

                    <div class="flex items-center text-4xl font-extrabold text-white px-2">-</div>

                    <div class="flex flex-col items-center gap-2 px-6">
                        <button @click="scoreOpponent < 8 && scoreOpponent++" class="text-white/70 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed" :disabled="scoreOpponent >= 8">▲</button>
                        <span class="text-5xl font-extrabold text-white">{{ scoreOpponent }}</span>
                        <button @click="scoreOpponent > 0 && scoreOpponent--" class="text-white/70 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed" :disabled="scoreOpponent == 0">▼</button>
                    </div>

                    <!-- valid form -->
                    <div class="absolute top-0 left-1/2 -translate-x-1/2">
                        <button @click="addMatch" class="cursor-pointer hover:scale-110 transition">
                            <addButton class="h-8" />
                        </button>
                    </div>


                    <div v-if="error" class="absolute w-full bottom-32 left-1/2 -translate-x-1/2 z-50 bg-red-500/90 text-white px-5 py-3 rounded-lg shadow-lg text-sm font-medium text-center">
                        {{ error }}
                    </div>
                </div>

            </div>


        </div>


    </main>
</template>
