<script setup>
// "Frise" of a game: every point in scoring order, one lane per player.
// Hover or focus a point to see who scored it, when, and the score after it.
import { computed } from "vue";
import PointIcon from "./PointIcon.vue";
import { formatClock } from "../lib/format.js";
import { POINT_LABEL, withRunningScore } from "../lib/matches.js";

const props = defineProps({
    events: { type: Array, required: true }, // EventView[] in seq order
    names: { type: Array, required: true },
    startedAt: { type: String, default: null },
});

const rows = computed(() => withRunningScore(props.events));
const at = (event) => (props.startedAt ? formatClock(Date.parse(event.occurredAt) - Date.parse(props.startedAt)) : "");
const describe = (event) =>
    `${props.names[event.seat - 1]} · ${POINT_LABEL[event.type]}${at(event) ? ` · ${at(event)}` : ""} · ${event.score[0]}–${event.score[1]}`;
</script>

<template>
    <div class="grid grid-cols-[auto_1fr] items-center gap-x-3 gap-y-1.5">
        <template v-for="seat in [1, 2]" :key="seat">
            <span class="max-w-24 truncate text-xs text-muted">{{ names[seat - 1] }}</span>
            <ol class="flex gap-[3px] overflow-x-auto" :aria-label="`Points de ${names[seat - 1]}`">
                <li v-for="event in rows" :key="event.id" class="size-[22px] shrink-0">
                    <span
                        v-if="event.seat === seat"
                        class="tick"
                        :style="{ '--c': `var(--chart-${event.type.toLowerCase()})` }"
                        :title="describe(event)"
                        :aria-label="describe(event)"
                        tabindex="0"
                    >
                        <PointIcon :type="event.type" class="size-3" />
                    </span>
                </li>
            </ol>
        </template>
    </div>
</template>

<style scoped>
.tick {
    display: grid;
    place-items: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--c);
    color: var(--surface);
    box-shadow: 0 0 0 2px var(--surface);
    cursor: default;
}
</style>
