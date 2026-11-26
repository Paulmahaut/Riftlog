# Riftlog v2 — Spécification

> Statut : brouillon — 2026-11-26

## 1. Vision

Riftlog est le compagnon de notre groupe de joueurs Riftbound :

- **Mobile** : compteur de points pendant les parties IRL, puis saisie d'après-partie.
- **PC** : historique, stats, decks, saisie manuelle des parties en ligne.

Les deux sont **une seule appli Vue** (PWA installable) avec des écrans adaptés à chaque support, branchée sur le même backend.

## 2. Décisions prises

| Sujet | Décision |
| --- | --- |
| Mode de jeu | 1v1 uniquement (pour l'instant) |
| Formats | Bo1 et Bo3, format par défaut enregistré dans le profil |
| Saisie en direct | Chaque joueur tape seulement **Conquer / Hold / Ability**, avec un bouton annuler |
| Pendant la partie | Qui commence, chrono (durée), Draw caché dans le menu `⋯` |
| Après la partie | Mulligan, battlefields, side : tout facultatif, modifiable ensuite depuis le PC |
| Téléphone | Un seul téléphone posé entre les joueurs, pas de synchro temps réel |
| Hors-ligne | La partie est stockée sur le téléphone, envoyée au serveur une fois terminée |
| Comptes | Les amis ont leur compte. Adversaires **invités** (simple nom) possibles |
| Visibilité | Tout utilisateur inscrit voit les parties et stats des autres |
| Decks (les nôtres) | Liste complète possible, à remplir à l'avance, jamais obligatoire |
| Deck adverse | La légende suffit |
| Données de cartes | Import depuis [Riftcodex](https://riftcodex.com) (voir §4) |
| Frontend | Réécrit avec une nouvelle DA. On garde la stack Vue 3 + Vite + Tailwind |

## 3. Stats visées (première version)

1. Winrate contre une légende donnée, selon qu'on commence ou non.
2. Répartition des points : conquer, hold et ability.
3. Battlefields sur lesquels on perd le plus.
4. Comebacks (exemple : gagner une partie où on était mené 2-5).

Toutes se calculent à partir des `score_events` et des infos de game. Aucun score final n'est stocké en dur.

## 4. Données de référence : Riftcodex

- API REST gratuite, sans clé : `https://api.riftcodex.com/cards?page=N&size=100`.
- Projet de fans non officiel, qui fonctionne sous la politique de contenu de fans de Riot.
- État au 2026-11-26 : 1 451 impressions, 8 sets. Après import et nettoyage : **49 légendes** et **64 battlefields** distincts. Le chiffre de 90 légendes donné plus tôt était faux : il comptait des variantes.
- Les champs utiles :
  - `id`, l'identifiant interne de Riftcodex, qui sert de clé externe. Le `riftbound_id` (ex. `ogn-001-298`) n'est **pas unique** : les versions « (Metal) » partagent celui de la carte de base, et certaines cartes sont listées deux fois ;
  - `name` et `tags`. Les légendes du set VEN n'ont pas le champion dans leur nom (« Defender of Tomorrow ») : l'import le reprend dans les `tags`, ce qui donne « Jayce - Defender of Tomorrow ». « Yordle, Kennen - … » devient « Kennen - … » ;
  - `classification.type` et `classification.domain` ;
  - `set.set_id` ;
  - `media.image_url` ;
  - `metadata.alternate_art`, `metadata.overnumbered` et `metadata.signature`.
- **On copie les cartes dans notre base** avec un import lancé à la main ou planifié. On n'appelle jamais Riftcodex en direct depuis l'appli. Raisons : le hors-ligne, et la dépendance à un service qui est encore en développement.
- Le filtrage par type se fait de notre côté, car l'API ne propose pas de filtre `type`.

## 5. Modèle de données (V3)

Les entités créées sur le téléphone hors-ligne ont un **UUID généré par le client**. L'envoi au serveur est ainsi idempotent : renvoyer la même partie ne crée pas de doublon.

```text
users            (existant) + default_format
cards            id, riftbound_id UNIQUE, name, base_name, type, set_id, domains, image_url, is_variant
guest_players    id, name, created_by → users

decks            id UUID, owner → users, name, legend → cards, notes, created_at
deck_cards       deck_id, card_id, quantity, zone (MAIN | RUNE | BATTLEFIELD | SIDE | CHAMPION)

matches          id UUID, format (BO1|BO3), source (LIVE|MANUAL|IMPORTED), external_id NULL,
                 recorded_by → users, played_at, winner_seat NULL (1|2|NULL = nul/inachevé)
match_players    match_id, seat (1|2), user_id NULL, guest_id NULL, deck_id NULL, legend → cards
                 (exactement un parmi user_id et guest_id)

games            id UUID, match_id, game_number, first_seat, started_at, ended_at,
                 result (SEAT1|SEAT2|DRAW)
game_players     game_id, seat, battlefield → cards NULL, mulligan_count NULL, notes NULL
game_side_changes game_id, seat, card_id, delta (+n entré / -n sorti)

score_events     id UUID, game_id, seq, seat, type (CONQUER|HOLD|ABILITY), occurred_at
```

Les tables `match_players` et `game_players` remplacent les colonnes « moi / adversaire ». Le passage au 2v2 restera possible sans tout casser.

## 6. Écrans

**Mobile**

- Lancement d'un match : format (par défaut celui du profil), adversaire (ami ou invité), mon deck, légende adverse, qui commence.
- Duel :
  - écran coupé en deux, la moitié du haut tournée à 180° ;
  - trois gros boutons par joueur, score affiché ;
  - au centre : chrono, annuler et menu `⋯` (Draw, abandon).
- Fin de game : bouton facultatif « compléter » (battlefields, mulligans, side), puis game suivante si Bo3.
- File d'envoi : parties en attente de synchro.

**PC**

- Connexion et inscription (manquantes en v1).
- Historique : filtres, détail d'une partie avec la frise des points.
- Stats (§3).
- Decks : builder avec recherche de cartes.
- Saisie manuelle d'un match (source `MANUAL`).

## 6 bis. Direction artistique

- **Mobile** : simple, efficace, agréable et sobre.
  - Ce qu'on garde de la référence (capture d'un compteur existant) : très gros scores, moitié du haut retournée, une couleur et une icône par type de point (Conquer, Hold, Ability), barre centrale avec chrono et indicateur de game, frise des points sur le côté.
  - On ne copie pas la référence : on reprend les principes et on crée notre propre identité visuelle.
- **PC, deux thèmes au choix** :
  - **Joli** : soigné, illustré. Référence : Papertree (lien à fournir).
  - **Lecture** : dense et neutre, tableaux et graphiques avant tout, pensé pour lire les données le plus vite possible.
- Conséquence technique : couleurs, espacements et typos définis dans des variables CSS (via Tailwind). Changer de thème revient à changer de jeu de variables, sans dupliquer les écrans.

## 7. Étapes

0. **Préparation** ✅ : la version scolaire est déjà taguée `alpha`. Branche git `Post-Uni` et branche Neon `v2` créées le 2026-11-26. La base Neon de prod ne contient que des données de test.
1. **Backend** ✅ (2026-11-26, hors stats) :
   - migrations V3 (schéma v2) et V4 (cartes identifiées par l'id Riftcodex) ;
   - import Riftcodex au premier démarrage, puis via `POST /api/cards/import` ;
   - endpoints v2 : envoi idempotent des matchs, decks, cartes, joueurs et invités (liste dans le README) ;
   - 40 tests, plus une vérification de bout en bout sur la branche Neon `v2` (PostgreSQL).
   - Les **stats** passent à l'étape 4, avec l'écran PC qui les affiche.
2. **Socle frontend** ✅ (2026-11-26) :
   - connexion et inscription ;
   - navigation mobile (barre du bas) et PC (barre latérale) ;
   - thèmes « table » et « lecture » ;
   - PWA installable qui s'ouvre hors-ligne ;
   - cache IndexedDB des légendes, battlefields et joueurs ;
   - file d'envoi des matchs, testée et vérifiée de bout en bout : match en attente hors-ligne, puis envoi automatique au retour du réseau.

   Le token JWT passe de 24 h à 30 jours. Détails dans `FRONTEND.md`.
3. **Duel mobile** et écran d'après-partie ✅ (2026-11-26) :
   - lancement : format, ma légende, adversaire (ami, invité connu ou nouvel invité), légende adverse ;
   - duel d'après la maquette : choix de qui commence, Conquer / Hold / Ability, annuler, chrono, Bo1 / Bo3, menu `⋯` (égalité, abandon, quitter, supprimer) ;
   - fin de game : courbe du score, liste des points, battlefields et mulligans facultatifs ;
   - fin de match : résumé, « Revanche », envoi automatique via la file d'envoi ;
   - le match en cours est sauvegardé sur le téléphone à chaque point : fermer l'appli ou recharger ne perd rien ;
   - vérifié de bout en bout (Bo3 complet, rechargement en cours de game, envoi au serveur).
4. **Historique et stats** sur PC.
5. **Deck builder**.
6. **Saisie manuelle** pour les tournois en ligne.
7. *Plus tard* : import Riftatlas (vérifier d'abord l'API et les conditions d'utilisation), 2v2.

## 7 bis. Règles de branche (en vue du merge `Post-Uni` → `main`)

La branche git `Post-Uni` sera fusionnée dans `main` en **gardant tous ses commits** (pas de squash). Pour que ça se passe sans problème :

**Git**
1. **On ne commite plus sur `main`** d'ici le merge. La fusion reste alors un fast-forward, sans conflit. Les branches `backend` et `launch` ne sont plus utilisées non plus.
2. **On pousse `Post-Uni` souvent.** Ça ne déclenche aucun déploiement.
3. **Des messages de commit clairs**, puisque tout l'historique sera conservé dans `main`.
4. Juste avant le merge, on vérifie que `main` n'a pas bougé : `git log Post-Uni..main` doit être vide. Si ce n'est pas le cas, on fait `git merge main` dans `Post-Uni` d'abord et on règle les conflits là.

**Base de données (Flyway)**

5. **On ne modifie jamais `V1__init_schema.sql` ni `V2__insertions.sql`.** La prod les a déjà appliqués et Flyway refuserait de démarrer.
6. Tout changement de schéma passe par un **nouveau fichier** : `V3__...`, `V4__...` Le V3 supprime les anciennes tables (données de test uniquement) et crée le schéma v2.
7. Chaque migration est d'abord testée sur la **branche Neon `v2`**, jamais directement sur la prod.

**Déploiement**

8. Render redéploie à chaque push sur `main` : **merger, c'est mettre en prod**, et la V3 s'applique automatiquement à la base de prod.
9. Avant le merge, il faut :
   - vérifier que la prod ne contient toujours rien d'important ;
   - mettre à jour les variables Render si besoin (`CORS_ALLOWED_ORIGINS` pour l'URL du nouveau frontend) ;
   - relire [DEPLOY.md](DEPLOY.md).
10. Après le merge, on vérifie que l'API répond. La branche Neon `v2` peut alors être supprimée.

## 8. Questions ouvertes

- ~~La base Neon de prod contient-elle des données à garder ?~~ Non (vérifié le 2026-11-26) : seulement des données de test (Alice/Bob, légendes fictives Ada/Jiro/Kaya, 10 matchs). On repart d'un schéma propre.
- DA PC « joli » : lien ou capture de Papertree.
- ~~Option pour ne pas télécharger les données ?~~ Inutile : tout ce qui est gardé sur l'appareil pèse environ 330 Ko (appli, polices et icônes) plus 37 Ko de données de jeu (7 Ko compressés). Aucune image de carte n'est téléchargée.
- L'hébergement : le plan gratuit de Render met le serveur en veille, ce qui ralentit le premier envoi.
- Riftatlas : API, conditions d'utilisation, comment fait Riftlite.
