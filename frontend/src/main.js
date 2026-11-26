import { createApp } from "vue";
import { registerSW } from "virtual:pwa-register";
import "./styles/main.css";
import App from "./App.vue";
import router from "./router";
import { applyTheme } from "./lib/theme.js";
import { startAutoSync } from "./lib/sync.js";

applyTheme();
createApp(App).use(router).mount("#app");
startAutoSync();

// Installs the service worker that lets the app open offline; updates apply on next launch.
registerSW({ immediate: true });
