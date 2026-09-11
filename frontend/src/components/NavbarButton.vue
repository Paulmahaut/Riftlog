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
    <button class="w-full h-[70px] flex items-center flex-shrink-0 self-stretch gap-[18px] p-[10px] cursor-pointer  transition-colors duration-200 hover:bg-[var(--buttonNavbar-bg-hover)] disabled:opacity-50 disabled:cursor-not-allowed" :disabled="disabled">
        <component v-if="icon" :is="icon" class="w-[28px] h-[28px]" />

        <span class="font-[var(--inter)] text-[18px] text-white font-[var(--Medium)] tracking-[6%]">{{ text }}</span>
    </button>
</template>