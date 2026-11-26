import { ref } from "vue";

const STORAGE_KEY = "riftlog.theme";

export const THEMES = [
    { id: "table", label: "Table", description: "Sombre et calme, lisible de l'autre côté de la table." },
    { id: "lecture", label: "Lecture", description: "Clair et dense, pour lire les données sur PC." },
];

function initialTheme() {
    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        return THEMES.some((theme) => theme.id === saved) ? saved : "table";
    } catch {
        return "table";
    }
}

export const currentTheme = ref(initialTheme());

export function applyTheme(id = currentTheme.value) {
    currentTheme.value = id;
    document.documentElement.dataset.theme = id;
    const background = getComputedStyle(document.documentElement).getPropertyValue("--bg").trim();
    document.querySelector('meta[name="theme-color"]')?.setAttribute("content", background);
    try {
        localStorage.setItem(STORAGE_KEY, id);
    } catch {
        // The choice just won't be remembered.
    }
}
