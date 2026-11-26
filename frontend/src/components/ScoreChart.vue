<script setup>
// Step chart of one game: each player's score after every point, in scoring order.
import { computed } from "vue";
import { WINNING_SCORE } from "../lib/duel.js";

const props = defineProps({
    /** [{ seat: 1|2 }] in scoring order */
    events: { type: Array, required: true },
    names: { type: Array, required: true }, // [seat 1 name, seat 2 name]
});

const W = 320;
const H = 130;
const L = 26;
const R = 8;
const T = 10;
const B = 22;

const n = computed(() => Math.max(props.events.length, 1));
const x = (k) => L + (k / n.value) * (W - L - R);
const y = (v) => T + (1 - Math.min(v, WINNING_SCORE) / WINNING_SCORE) * (H - T - B);

const paths = computed(() =>
    [1, 2].map((seat) => {
        let total = 0;
        let d = `M${x(0)} ${y(0)}`;
        props.events.forEach((event, k) => {
            if (event.seat === seat) {
                total++;
                d += ` H${x(k + 1)} V${y(total)}`;
            }
        });
        return `${d} H${x(n.value)}`;
    }),
);
</script>

<template>
    <figure class="grid gap-2">
        <svg :viewBox="`0 0 ${W} ${H}`" class="block h-auto w-full" role="img" aria-label="Évolution du score point par point">
            <g v-for="v in [0, 4, 8]" :key="v">
                <line :x1="L" :x2="W - R" :y1="y(v)" :y2="y(v)" stroke="var(--line)" stroke-width="1" />
                <text :x="L - 8" :y="y(v) + 4" text-anchor="end" font-size="11" fill="var(--muted)">{{ v }}</text>
            </g>
            <text :x="W - R" :y="H - 4" text-anchor="end" font-size="11" fill="var(--muted)">
                {{ events.length }} point{{ events.length > 1 ? "s" : "" }} marqué{{ events.length > 1 ? "s" : "" }}
            </text>
            <path :d="paths[1]" fill="none" stroke="var(--seat-opp)" stroke-width="2.5" stroke-linejoin="round" />
            <path :d="paths[0]" fill="none" stroke="var(--seat-me)" stroke-width="2.5" stroke-linejoin="round" />
        </svg>
        <figcaption class="flex gap-4 text-xs text-muted">
            <span class="flex items-center gap-1.5"><i class="inline-block h-[3px] w-2.5 rounded bg-seat-me" />{{ names[0] }}</span>
            <span class="flex items-center gap-1.5"><i class="inline-block h-[3px] w-2.5 rounded bg-seat-opp" />{{ names[1] }}</span>
        </figcaption>
    </figure>
</template>
