// Generates the PNG app icons from public/favicon.svg.
// Run `npm run icons` after changing the logo, then commit the PNG files in public/.
import { defineConfig, minimal2023Preset } from "@vite-pwa/assets-generator/config";

export default defineConfig({
    preset: {
        ...minimal2023Preset,
        maskable: { ...minimal2023Preset.maskable, resizeOptions: { background: "#0E1115" } },
        apple: { ...minimal2023Preset.apple, resizeOptions: { background: "#0E1115" } },
    },
    images: ["public/favicon.svg"],
});
