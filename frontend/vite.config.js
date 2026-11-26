import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
    plugins: [
        vue(),
        tailwindcss(),
        // Installable app that opens without network: the app shell (HTML, JS, CSS, fonts)
        // is cached by a service worker. Data lives in IndexedDB (src/lib/db.js), not here.
        VitePWA({
            registerType: "autoUpdate",
            includeAssets: ["favicon.svg", "apple-touch-icon-180x180.png"],
            manifest: {
                name: "Riftlog",
                short_name: "Riftlog",
                description: "Compteur de points et historique de parties Riftbound",
                lang: "fr",
                start_url: "/",
                display: "standalone",
                orientation: "portrait",
                background_color: "#0E1115",
                theme_color: "#0E1115",
                icons: [
                    { src: "pwa-64x64.png", sizes: "64x64", type: "image/png" },
                    { src: "pwa-192x192.png", sizes: "192x192", type: "image/png" },
                    { src: "pwa-512x512.png", sizes: "512x512", type: "image/png" },
                    { src: "maskable-icon-512x512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
                ],
            },
            workbox: {
                globPatterns: ["**/*.{js,css,html,svg,png,woff2}"],
                navigateFallback: "/index.html",
                // API calls always go to the network; offline data is handled by the app itself.
                navigateFallbackDenylist: [/^\/api\//],
            },
        }),
    ],
    server: {
        proxy: {
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: true,
            },
        },
    },
    test: {
        environment: "node",
        setupFiles: ["./src/test/setup.js"],
    },
});
