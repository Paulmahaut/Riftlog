<script setup>
import { computed } from "vue";
import { FORMAT_LABEL, OUTCOME_LABEL, formatDateTime, outcomeFor } from "../lib/format.js";
import { session } from "../lib/session.js";

const props = defineProps({
    match: { type: Object, required: true },
});

const outcome = computed(() => outcomeFor(props.match, session.user?.id));
const seat = (n) => props.match.players.find((player) => player.seat === n);

const OUTCOME_TONE = {
    win: "bg-success/15 text-success",
    loss: "bg-danger/15 text-danger",
    draw: "bg-surface-2 text-muted",
};
</script>

<template>
    <article class="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-1 px-4 py-3">
        <div class="min-w-0">
            <p class="truncate text-sm">
                <span class="font-semibold" :class="match.winnerSeat === 1 ? 'text-fg' : 'text-muted'">{{ seat(1)?.name }}</span>
                <span class="text-muted"> · {{ seat(1)?.legend?.name }}</span>
            </p>
            <p class="truncate text-sm">
                <span class="font-semibold" :class="match.winnerSeat === 2 ? 'text-fg' : 'text-muted'">{{ seat(2)?.name }}</span>
                <span class="text-muted"> · {{ seat(2)?.legend?.name }}</span>
            </p>
        </div>
        <div class="grid justify-items-end gap-1">
            <span class="font-display text-2xl font-bold leading-none tabular-nums">
                {{ match.gameWins[0] }}–{{ match.gameWins[1] }}
            </span>
            <span v-if="outcome" class="rounded-full px-2 py-0.5 text-[11px] font-semibold" :class="OUTCOME_TONE[outcome]">
                {{ OUTCOME_LABEL[outcome] }}
            </span>
        </div>
        <p class="col-span-2 text-xs text-muted">
            {{ formatDateTime(match.playedAt) }} · {{ FORMAT_LABEL[match.format] }}
            <template v-if="match.source !== 'LIVE'"> · saisi à la main</template>
        </p>
    </article>
</template>
