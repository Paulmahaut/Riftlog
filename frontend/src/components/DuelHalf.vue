<script setup>
// One player's half of the duel screen: score, the three point buttons and their recent points.
// The opponent's half is the same component, rotated 180° so it faces them across the table.
import { computed } from "vue";
import PointIcon from "./PointIcon.vue";
import { POINT_TYPES, WINNING_SCORE } from "../lib/duel.js";
import { legendShort } from "../lib/format.js";

const props = defineProps({
    seat: { type: Number, required: true },
    slot: { type: Object, required: true }, // { name, legendName, kind }
    game: { type: Object, required: true },
    phase: { type: String, required: true },
    flipped: { type: Boolean, default: false },
});
const emit = defineEmits(["point", "pick"]);

const LABEL = { CONQUER: "Conquer", HOLD: "Hold", ABILITY: "Ability" };

const mine = computed(() => props.game.events.filter((event) => event.seat === props.seat));
const points = computed(() => mine.value.length);
const recent = computed(() => mine.value.slice(-12).reverse());
const guest = computed(() => props.slot.kind !== "user");
</script>

<template>
    <section
        class="half"
        :class="[flipped ? 'half-opp' : 'half-me', { 'rotate-180': flipped }]"
        :aria-label="`${slot.name}, ${points} point${points > 1 ? 's' : ''}`"
    >
        <div class="main">
            <div class="score" aria-hidden="true">{{ points }}</div>
            <div class="flex gap-1.5" aria-hidden="true">
                <span v-for="i in WINNING_SCORE" :key="i" class="pip" :class="{ on: i <= points }" />
            </div>
            <div class="flex items-baseline gap-2 text-[13px] text-muted">
                <b class="name font-semibold">{{ slot.name }}</b>
                <span>{{ legendShort(slot.legendName) }}{{ guest ? " · invité" : "" }}</span>
                <span v-if="game.firstSeat === seat" class="badge">1er</span>
            </div>
            <div class="flex gap-3.5">
                <button
                    v-for="type in POINT_TYPES"
                    :key="type"
                    type="button"
                    class="act"
                    :style="{ '--c': `var(--${type.toLowerCase()})` }"
                    :disabled="phase !== 'playing'"
                    :aria-label="`${LABEL[type]} pour ${slot.name}`"
                    @click="emit('point', type)"
                >
                    <span class="disc"><PointIcon :type="type" class="size-7" /></span>
                    {{ LABEL[type] }}
                </button>
            </div>
        </div>

        <ol class="strip" aria-label="Derniers points">
            <li
                v-for="event in recent"
                :key="event.id"
                class="tick"
                :style="{ '--c': `var(--${event.type.toLowerCase()})` }"
                :title="LABEL[event.type]"
            >
                <PointIcon :type="event.type" class="size-[13px]" />
            </li>
        </ol>

        <div v-if="phase === 'pick-first'" class="pick">
            <button type="button" class="pick-btn" @click="emit('pick')">
                <span class="font-display text-[34px] font-bold">Je commence</span>
                <span class="text-[13px] text-muted">
                    {{ slot.name }}{{ game.number > 1 ? " · le perdant de la game précédente choisit" : "" }}
                </span>
            </button>
        </div>
    </section>
</template>

<style scoped>
.half {
    position: relative;
    min-height: 0;
    display: grid;
    grid-template-columns: 1fr 30px;
    gap: 8px;
    padding-block: 14px 18px;
    padding-left: max(16px, env(safe-area-inset-left, 0px));
    padding-right: max(12px, env(safe-area-inset-right, 0px));
}
.half-me {
    background: color-mix(in srgb, var(--seat-me) 4%, var(--bg));
}
.half-opp {
    background: color-mix(in srgb, var(--seat-opp) 4%, var(--bg));
}
.main {
    display: grid;
    grid-template-rows: 1fr auto auto auto;
    justify-items: center;
    align-items: center;
    gap: 10px;
    min-height: 0;
}
.score {
    font-family: var(--font-display);
    font-weight: 700;
    font-size: clamp(88px, 21vh, 210px);
    line-height: 0.82;
    font-variant-numeric: tabular-nums;
    letter-spacing: -0.02em;
}
.pip {
    width: 18px;
    height: 5px;
    border-radius: 3px;
    background: var(--line);
}
.half-me .pip.on {
    background: var(--seat-me);
}
.half-me .name {
    color: var(--seat-me);
}
.half-opp .pip.on {
    background: var(--seat-opp);
}
.half-opp .name {
    color: var(--seat-opp);
}
.badge {
    font-family: var(--font-display);
    font-weight: 600;
    font-size: 12px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    padding: 1px 7px;
    border-radius: 4px;
    border: 1px solid currentColor;
}
.act {
    width: 86px;
    display: grid;
    justify-items: center;
    gap: 6px;
    padding: 4px 0;
    font-family: var(--font-display);
    font-weight: 600;
    font-size: 13px;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: var(--muted);
    touch-action: manipulation;
}
.disc {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    display: grid;
    place-items: center;
    background: var(--c);
    color: var(--bg);
    transition: transform 0.1s ease;
}
.act:active .disc {
    transform: scale(0.9);
}
.act:disabled {
    opacity: 0.35;
}
.strip {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 2px 0 0 6px;
    margin: 0;
    list-style: none;
    overflow: hidden;
    border-left: 1px solid var(--line);
}
.tick {
    width: 22px;
    height: 22px;
    flex: none;
    border-radius: 50%;
    display: grid;
    place-items: center;
    background: var(--c);
    color: var(--bg);
}
.tick:nth-child(n + 6) {
    opacity: 0.7;
}
.tick:nth-child(n + 9) {
    opacity: 0.4;
}
.pick {
    position: absolute;
    inset: 0;
    z-index: 3;
    display: grid;
    place-items: center;
    background: color-mix(in srgb, var(--bg) 78%, transparent);
    backdrop-filter: blur(3px);
}
.pick-btn {
    display: grid;
    gap: 6px;
    justify-items: center;
    padding: 22px 30px;
    border-radius: 14px;
    border: 1px solid var(--line);
    background: var(--surface);
}
</style>
