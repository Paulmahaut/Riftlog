import { createRouter, createWebHistory } from "vue-router";

import Home from "../views/Home.vue";
import Play from "../views/Play.vue";
import Stats from "../views/Stats.vue";
import Decks from "../views/Decks.vue";
import Settings from "../views/Settings.vue";

const router = createRouter({
    history: createWebHistory(),

    routes: [
        {
            path: "/",
            component: Home,
        },
        {
            path: "/play",
            component: Play,
        },
        {
            path: "/stats",
            component: Stats,
        },
        {
            path: "/decks",
            component: Decks,
        },
        {
            path: "/settings",
            component: Settings,
        },
    ],
});

export default router;
