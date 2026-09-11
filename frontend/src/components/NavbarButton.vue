<script setup>
defineProps({
    text: {
        type: String,
        required: true
    },

    icon: {
        type: Object,
        default: null
    },

    disabled: {
        type: Boolean,
        default: false
    }
})
</script>

<template>
    <button class="button" :disabled="disabled">
        <component v-if="icon" :is="icon" class="icon" />

        <span>{{ text }}</span>
    </button>
</template>

<style scoped>
.button {
    width: 100%;
    height: 70px;

    display: flex;
    align-items: center;
    flex-shrink: 0;
    align-self: stretch;
    gap: 18px;

    padding: 10px;

    border: none;

    cursor: pointer;
}

.icon {
    width: 28px;
    height: 28px;
}

span {
    font-family: var(--inter);
    font-size: 18px;
    font-weight: var(--Medium);
    letter-spacing: 6%;
}

.button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}
</style>