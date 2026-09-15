> Document en français, à but purement informatif pour le frontdev

État actuel du code — quoi remplacer où

  Le projet Vue existe déjà dans frontend/ (Vite + Tailwind + vue-router), lancé avec `npm run dev`
  (http://localhost:5173). Ce qui existe et ce qu'il reste à faire, fichier par fichier :

  frontend/src/views/Play.vue      vide (juste un titre) — construire le formulaire de log de match
                                    (écran 1 ci-dessous). C'est l'écran principal.
  frontend/src/views/Home.vue      vide — à toi de voir ce qu'il affiche (accueil / raccourci vers
                                    Play ? historique des matchs ?), pas de contrainte imposée.
  frontend/src/views/Stats.vue     vide — afficher GET /api/stats (écran 3 ci-dessous).
  frontend/src/views/Decks.vue     vide — lister/créer des decks (GET/POST /api/decks).
  frontend/src/views/Settings.vue  vide — hors scope v1 (voir "Hors scope"), laisse-le vide pour
                                    l'instant, pas besoin d'y construire quoi que ce soit.
  frontend/src/router/index.js     déjà fonctionnel, une route par écran ci-dessus — rien à changer
                                    sauf si tu ajoutes un nouvel écran.
  frontend/src/api.js              déjà fait, ne pas toucher au fonctionnement interne. Utilise la
                                    fonction qu'il exporte (voir "Authentification" plus bas) au lieu
                                    du `fetch` natif à chaque fois que tu appelles le backend.

Glossaire (aucune connaissance du jeu supposée)

  Legend       Un personnage/héros. Exemple : "Ashe", "Viktor". Chaque deck est construit autour
               d'exactement un Legend.
  Deck         Un deck nommé, lié à un Legend. Exemple : "Ashe Aggro" (construit autour d'Ashe).
  Match        Une partie complète entre l'utilisateur et un adversaire, jouée en vrai avec des
               cartes physiques.
  Round        Un point d'étape dans un match. Le score est cumulatif — il ne fait qu'augmenter
               pendant un match.
  Victoire     Un match se termine dès qu'un des deux camps atteint 8 points au dernier round
               enregistré. Celui qui a le plus de points à ce moment-là a gagné.

À quoi sert l'appli

  Juste après avoir fini un duel de cartes physique, l'utilisateur ouvre ça sur son téléphone et
  enregistre le résultat en quelques taps : contre qui il a joué, quels decks ont été utilisés, et
  le score à la fin de chaque round. La rapidité compte — ça se passe entre deux parties à une
  table, pas à un bureau.

Écrans à construire (scope v1 — rien au-delà de cette liste)

  1. Logger un match (l'écran principal, utilisé à chaque duel)
     Champs : nom de l'adversaire, mon legend, mon deck, legend de l'adversaire, deck de l'adversaire
     (tout en texte libre — voir "Autocomplétion" plus bas), et une liste de rounds que l'utilisateur
     peut ajouter un par un (le numéro de round est juste la position dans la liste — pas besoin que
     l'utilisateur le tape) :
       - Ligne de round : mon score cumulé, score cumulé de l'adversaire
     Un bouton "logger le match" qui envoie tout d'un coup (voir POST /api/matches plus bas).
     Côté client : bloquer l'envoi si les scores du dernier round n'ont pas un camp à 8+ — l'API le
     refusera de toute façon (400), mais l'attraper avant l'envoi évite un aller-retour réseau inutile.

  2. Historique des matchs
     Une liste des matchs passés (voir GET /api/matches), du plus récent au plus ancien si possible
     (pas obligatoire). Afficher au minimum : date, adversaire, les deux decks, résultat (win/loss),
     score final.

  3. Stats
     Afficher ce que renvoie GET /api/stats : taux de victoire global, par deck, par matchup. Une
     simple liste/tableau suffit — pas de graphiques nécessaires pour la v1.

  Autocomplétion (bonus, pas obligatoire pour la v1) : GET /api/decks et GET /api/legends renvoient
  tout ce qui a déjà été loggé, donc les champs "mon deck" / "legend" pourraient suggérer des noms
  existants plutôt que de forcer l'utilisateur à retaper "Ashe Aggro" à chaque fois. Pas obligatoire
  pour livrer la v1 sans ça.

Hors scope explicite pour la v1 — à ne pas construire

  Pas d'écran login/register (un seul compte partagé, connexion automatique — voir
  "Authentification" ci-dessous). Pas de modification ou suppression d'un
  match déjà loggé. Pas de replays, vidéo, ou détail carte par carte — c'est du tracking de score,
  pas le jeu entier. Pas besoin de support hors-ligne (on suppose que le téléphone a accès au réseau
  pour joindre le backend au moment de logger).

Authentification

  Le backend exige un token sur chaque appel /api/**, mais tu n'as rien à gérer toi-même :
  frontend/src/api.js s'en occupe (connexion automatique avec un compte de démo partagé, ajout du
  header à chaque requête). Utilise juste `apiFetch` à la place de `fetch` :
    import { apiFetch } from '../api.js'
    const response = await apiFetch('/api/matches')
  Même signature que fetch (méthode, body, etc. dans le deuxième argument), donc rien d'autre à
  apprendre.

Référence API

  Toutes les requêtes/réponses sont en JSON.

  POST /api/matches — logger un match complet. Corps de la requête :
    {
      "opponentName": "Julien",
      "myLegendName": "Ashe",
      "myDeckName": "Ashe Aggro",
      "opponentLegendName": "Viktor",
      "opponentDeckName": "Viktor Control",
      "rounds": [
        { "roundNumber": 1, "myScore": 3, "opponentScore": 0 },
        { "roundNumber": 2, "myScore": 5, "opponentScore": 2 },
        { "roundNumber": 3, "myScore": 8, "opponentScore": 4 }
      ]
    }
  Réponse (201 Created) : même forme qu'un élément de GET /api/matches ci-dessous.
  En cas d'erreur (400) : { "timestamp": "...", "status": 400, "error": "Bad Request",
                     "message": "Match must end with one side reaching 8 points", "path": "/api/matches" }
  — même forme d'erreur 400 pour un champ manquant/vide, avec le nom du champ dans "message".

  GET /api/matches — historique des matchs. Paramètres optionnels : ?opponentId= ou ?deckId= (des
  ids numériques, pas des noms — utile seulement après avoir aussi appelé GET /api/decks pour en
  retrouver un). Réponse :
    [
      {
        "id": 1,
        "playedAt": "2026-09-11T08:55:36.235243",
        "opponentName": "Julien",
        "myLegendName": "Ashe",
        "myDeckName": "Ashe Aggro",
        "opponentLegendName": "Viktor",
        "opponentDeckName": "Viktor Control",
        "result": "WIN",
        "myFinalScore": 8,
        "opponentFinalScore": 4,
        "rounds": [
          { "id": 1, "roundNumber": 1, "myScore": 3, "opponentScore": 0 },
          { "id": 2, "roundNumber": 2, "myScore": 5, "opponentScore": 2 },
          { "id": 3, "roundNumber": 3, "myScore": 8, "opponentScore": 4 }
        ]
      }
    ]

  GET /api/matches/{id} — un match, même forme qu'un élément du tableau ci-dessus.

  GET /api/stats — réponse :
    {
      "totalMatches": 2,
      "overallWinRate": 0.5,
      "winRateByDeck": { "Ashe Aggro": 0.5 },
      "winRateByMatchup": { "Ashe Aggro vs Viktor Control": 1.0, "Ashe Aggro vs Zoe Tempo": 0.0 }
    }
  Les taux de victoire sont entre 0.0 et 1.0, multiplier par 100 pour un pourcentage.

  GET /api/decks / POST /api/decks — { "name": "Ashe Aggro", "legendName": "Ashe" } →
    { "id": 1, "name": "Ashe Aggro", "legendName": "Ashe" }. Poster un nom qui existe déjà pour ce
    legend renvoie simplement celui qui existe déjà — sûr d'appeler sans vérifier avant.

  GET /api/legends / POST /api/legends — { "name": "Ashe" } → { "id": 1, "name": "Ashe" }. Même
  comportement de réutilisation si ça existe déjà.

Attentes techniques

  Mobile-first — c'est utilisé sur un téléphone à une table, pas sur un bureau. Le choix du
  framework est libre (HTML/CSS/JS brut, React, Vue, ce que tu préfères) — il y a déjà un exemple
  jetable en JS brut dans backend/src/main/resources/static/index.html (lecture seule, pas de
  formulaire) si tu veux voir l'API appelée depuis le navigateur.

  Où mettre le code : place un vrai projet frontend dans frontend/ à la racine du repo (à côté de
  backend/) plutôt que de faire grossir backend/src/main/resources/static — ce chemin convient pour
  une page de debug d'un seul fichier, pas pour une vraie appli avec un build. Pointe vers l'URL de
  base du backend (configurable, car les hôtes de dev/prod seront différents) plutôt que de supposer
  la même origine.
