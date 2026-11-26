<script setup>
// Share of Conquer / Hold / Ability in a set of points: one stacked bar, labelled underneath.
import { computed } from "vue";
import PointIcon from "./PointIcon.vue";
import { POINT_LABEL } from "../lib/matches.js";

const props = defineProps({
    label: { type: String, required: true },
    split: { type: Object, required: true }, // { conquer, hold, ability, games }
});

const total = computed(() => props.split.conquer + props.split.hold + props.split.ability);
const parts = computed(() =>
    ["CONQUER", "HOLD", "ABILITY"].map((type) => {
        const count = props.split[type.toLowerCase()];
        return { type, count, share: total.value ? Math.round((count / total.value) * 100) : 0 };
    }),
);
const perGame = computed(() => (props.split.games ? (total.value / props.split.games).toFixed(1).replace(".", ",") : "—"));
</script>

<template>
    <div class="grid gap-2">
        <div class="flex items-baseline justify-between gap-3 text-sm">
            <span class="font-semibold">{{ label }}</span>
            <span class="text-muted tabular-nums">{{ total }} points · {{ perGame }} par game</span>
        </div>
        <div v-if="total" class="flex h-3 gap-[2px]" aria-hidden="true">
            <span
                v-for="part in parts.filter((p) => p.count)"
                :key="part.type"
                class="rounded-[3px]"
                :style="{ flexGrow: part.count, background: `var(--chart-${part.type.toLowerCase()})` }"
                :title="`${POINT_LABEL[part.type]} : ${part.count} (${part.share} %)`"
            />
        </div>
        <div v-else class="h-3 rounded-[3px] bg-surface-2" aria-hidden="true" />
        <ul class="flex flex-wrap gap-x-5 gap-y-1 text-xs text-muted">
            <li v-for="part in parts" :key="part.type" class="flex items-center gap-1.5 tabular-nums">
                <span class="grid size-4 place-items-center rounded-full text-surface" :style="{ background: `var(--chart-${part.type.toLowerCase()})` }">
                    <PointIcon :type="part.type" class="size-2.5" />
                </span>
                <span class="text-fg">{{ POINT_LABEL[part.type] }}</span> {{ part.count }} · {{ part.share }} %
            </li>
        </ul>
    </div>
</template>
