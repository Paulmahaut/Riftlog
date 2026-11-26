import { createRouter, createWebHistory } from "vue-router";
import { isLoggedIn } from "../lib/session.js";

// meta.public: reachable without an account. Every other route needs a session
// (an expired one is fine: the app keeps working offline and asks to log in again to upload).
const routes = [
    { path: "/login", name: "login", component: () => import("../views/LoginView.vue"), meta: { public: true, title: "Connexion" } },
    { path: "/register", name: "register", component: () => import("../views/RegisterView.vue"), meta: { public: true, title: "Créer un compte" } },
    { path: "/", name: "home", component: () => import("../views/HomeView.vue"), meta: { title: "Accueil" } },
    { path: "/play", name: "play", component: () => import("../views/PlayView.vue"), meta: { title: "Jouer" } },
    { path: "/matches", name: "matches", component: () => import("../views/MatchesView.vue"), meta: { title: "Matchs" } },
    { path: "/decks", name: "decks", component: () => import("../views/DecksView.vue"), meta: { title: "Decks" } },
    { path: "/settings", name: "settings", component: () => import("../views/SettingsView.vue"), meta: { title: "Réglages" } },
    { path: "/:pathMatch(.*)*", redirect: "/" },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach((to) => {
    if (!to.meta.public && !isLoggedIn()) {
        return { name: "login", query: to.fullPath === "/" ? {} : { redirect: to.fullPath } };
    }
    if (to.meta.public && isLoggedIn() && !to.query.again) {
        return { name: "home" };
    }
    return true;
});

router.afterEach((to) => {
    document.title = to.meta.title ? `${to.meta.title} · Riftlog` : "Riftlog";
});

export default router;
