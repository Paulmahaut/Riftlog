<script setup>
// Icon of a point type: flag (Conquer), shield (Hold), sparkles (Ability).
defineProps({
    type: { type: String, required: true }, // CONQUER | HOLD | ABILITY
});
</script>

<template>
    <svg viewBox="0 0 24 24" aria-hidden="true">
        <path
            v-if="type === 'CONQUER'"
            d="M6 21V4h11l-2 4 2 4H6"
            fill="none"
            stroke="currentColor"
            stroke-width="2.4"
            stroke-linejoin="round"
            stroke-linecap="round"
        />
        <path v-else-if="type === 'HOLD'" d="M12 3l7 3v5c0 4.8-3.2 8.3-7 10-3.8-1.7-7-5.2-7-10V6z" fill="currentColor" />
        <template v-else>
            <path d="M11 2.5l1.9 5.6 5.6 1.9-5.6 1.9L11 17.5l-1.9-5.6L3.5 10l5.6-1.9z" fill="currentColor" />
            <path d="M18.5 14.5l.9 2.6 2.6.9-2.6.9-.9 2.6-.9-2.6-2.6-.9 2.6-.9z" fill="currentColor" />
        </template>
    </svg>
</template>
