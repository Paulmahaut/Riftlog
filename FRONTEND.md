# Frontend Riftlog

Une seule appli Vue pour le téléphone et le PC, installable (PWA) et utilisable sans réseau.
Le téléphone sert à compter les points à la table, le PC à consulter et gérer. Voir `SPEC-v2.md`.

## Lancer

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, /api est redirigé vers le backend sur :8080
npm test           # tests unitaires (Vitest)
npm run build      # build de production dans dist/
npm run preview    # sert dist/ sur :4173, avec le service worker (pour tester le hors-ligne)
```

Pour tester sur un téléphone en local : `npm run dev -- --host`, puis ouvrir l'adresse « Network »
affichée par Vite. Le hors-ligne ne fonctionne qu'avec `build` + `preview` (ou en prod) : en mode
`dev`, il n'y a pas de service worker.

`npm run preview` tourne sur le port 4173 : le backend doit l'autoriser, avec
`CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:4173`.

En production, le build a besoin de l'URL de l'API : `VITE_API_URL=https://riftlog-backend.onrender.com`.

## Stack

| Outil | Rôle |
| --- | --- |
| Vue 3 + vue-router | Écrans et navigation |
| Tailwind CSS 4 | Styles, à partir des tokens de thème |
| vite-plugin-pwa | Service worker et manifest : appli installable qui s'ouvre sans réseau |
| idb | IndexedDB : cartes, joueurs et matchs en attente stockés sur l'appareil |
| Vitest + fake-indexeddb | Tests de la logique (file d'envoi…) |
| @fontsource | Polices embarquées (Inter, Barlow Condensed), donc disponibles hors-ligne |

## Organisation

```text
src/
├── main.js              démarrage : thème, routeur, synchro auto, service worker
├── App.vue              pages publiques (connexion) ou AppShell
├── router/index.js      routes ; toute page non `public` exige une session
├── styles/main.css      tokens de thème + petites classes partagées (.btn, .card, .input…)
├── lib/                 logique sans interface
│   ├── api.js           SEUL point d'accès au backend : api(path, { method, body })
│   ├── session.js       utilisateur connecté et token (localStorage)
│   ├── auth.js          connexion, inscription, profil, format par défaut
│   ├── db.js            base IndexedDB (cards, players, outbox, meta)
│   ├── reference.js     cache des légendes, battlefields, joueurs et invités
│   ├── outbox.js        file des matchs à envoyer (+ outbox.test.js)
│   ├── sync.js          envoi et rafraîchissement automatiques quand le réseau est là
│   ├── theme.js         thèmes « table » et « lecture »
│   └── format.js        dates, résultats, libellés
├── components/          AppShell (navigation), SyncBadge, MatchList, MatchRow, AppIcon…
└── views/               une page par route
```

## Règles à garder

**Couleurs** : jamais de couleur en dur dans un composant. On utilise les tokens
(`bg-surface`, `text-muted`, `border-line`, `text-conquer`…) définis dans `styles/main.css`.
Un nouveau thème, c'est juste un nouveau bloc `[data-theme="..."]`.

**Backend** : toujours passer par `api()` de `lib/api.js`, jamais `fetch` directement.
Il ajoute le token, et transforme les erreurs en `ApiError` (le serveur a répondu une erreur)
ou en `NetworkError` (serveur injoignable). `describeError(e)` donne un message en français.

**Hors-ligne** : un écran qui doit marcher à la table lit ses données dans IndexedDB
(`cachedCards`, `cachedPlayers`) et jamais directement depuis l'API.

**Matchs** : un match terminé n'est jamais envoyé directement. Il passe par
`enqueueMatch(userId, id, body)`, puis `syncNow()` l'envoie quand c'est possible. L'id est un UUID généré sur le
téléphone (`crypto.randomUUID()`), donc renvoyer un match ne crée jamais de doublon côté serveur.
Un match refusé par le serveur (400) reste sur l'appareil avec la raison, visible dans Réglages.

**Session** : le token dure 30 jours. S'il expire, l'appli continue de marcher hors-ligne
et un bandeau demande de se reconnecter pour envoyer les matchs.

## État

- Étape 2 (socle) : connexion, inscription, navigation mobile et PC, thèmes, PWA, cache hors-ligne,
  file d'envoi. ✅
- Étape 3 : l'écran de duel (`views/PlayView.vue` est pour l'instant un écran d'attente).
- Étape 4 : détail d'un match et statistiques. Étape 5 : deck builder.
