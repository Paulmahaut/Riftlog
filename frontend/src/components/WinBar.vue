<script setup>
// Win rate as a thin meter, with the number written next to it (never color alone).
import { computed } from "vue";
import { winRate } from "../lib/matches.js";

const props = defineProps({
    tally: { type: Object, required: true }, // { played, wins, losses, draws }
});

const rate = computed(() => winRate(props.tally));
const detail = computed(() => {
    const t = props.tally;
    return `${t.wins} V · ${t.losses} D${t.draws ? ` · ${t.draws} N` : ""}`;
});
</script>

<template>
    <div class="flex min-w-32 items-center gap-2.5" :title="detail">
        <span class="w-12 shrink-0 whitespace-nowrap text-right tabular-nums">{{ rate == null ? "—" : `${rate} %` }}</span>
        <span class="relative h-1.5 flex-1 rounded-full bg-surface-2" aria-hidden="true">
            <span v-if="rate" class="absolute inset-y-0 left-0 rounded-full bg-chart-me" :style="{ width: `${rate}%` }" />
            <!-- 50 % marker: above or below even at a glance -->
            <span class="absolute inset-y-[-3px] left-1/2 w-px bg-line" />
        </span>
    </div>
</template>
