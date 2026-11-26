<script setup>
defineProps({
    title: { type: String, required: true },
    subtitle: { type: String, default: "" },
});
</script>

<template>
    <div
        class="grid min-h-full place-items-center px-4 py-10"
        style="padding-top: max(2.5rem, env(safe-area-inset-top)); padding-bottom: max(2.5rem, env(safe-area-inset-bottom))"
    >
        <div class="grid w-full max-w-sm gap-8">
            <div class="grid justify-items-center gap-3 text-center">
                <img src="/favicon.svg" alt="" class="size-14" />
                <div>
                    <h1 class="font-display text-3xl font-bold tracking-wide">{{ title }}</h1>
                    <p v-if="subtitle" class="mt-1 text-sm text-muted">{{ subtitle }}</p>
                </div>
            </div>
            <slot />
        </div>
    </div>
</template>
