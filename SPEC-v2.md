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

Toutes se calculent à partir des `score_events` et des infos de game, à chaque appel de `GET /api/stats` (pas de cache : le groupe est petit). Un comeback = une game gagnée après avoir été mené d'au moins 3 points.

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

0. **Préparation** ✅ : la version scolaire est déjà taguée `alpha`, et sa base sauvegardée dans la branche Neon `v1-backup`. Branche git `Post-Uni` et branche Neon `v2` créées le 2026-11-26. La base Neon de prod ne contient que des données de test.
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
4. **Historique et stats** ✅ (2026-11-26) :
   - historique filtrable (les miens ou tout le groupe, format, légende, résultat) ;
   - détail d'un match : courbe du score et frise des points de chaque game, points par type, battlefields, mulligans, notes ; les joueurs du match peuvent compléter ces détails ou supprimer le match ;
   - `GET /api/stats?userId=` calculé à partir des points stockés, et page Stats : winrate contre chaque légende en commençant ou non, répartition Conquer / Hold / Ability marqués et encaissés, battlefields les plus perdus (les siens et ceux de l'adversaire), comebacks (gagner après avoir été mené de 3 points ou plus) ;
   - couleurs des graphiques vérifiées (daltonisme, contraste) pour chaque thème ;
   - sur téléphone, l'onglet Decks laisse sa place à Stats (Decks reste sur PC).
5. **Deck builder**.
6. **Saisie manuelle** pour les tournois en ligne.
7. *Plus tard* : import Riftatlas (vérifier d'abord l'API et les conditions d'utilisation), 2v2.

## 7 bis. Branches et mise en prod

**Décision du 2026-11-26** : la v2 passe en prod sans préprod séparée. La prod ne contient que des données de test et n'a pas encore de vrais utilisateurs, donc elle sert de préprod. La version scolaire reste disponible : tag git `alpha` et branche Neon `v1-backup`.

**Le merge `Post-Uni` → `main`**

1. Le merge garde **tous les commits** (fast-forward, pas de squash). Vérifié le 2026-11-26 : `git log Post-Uni..origin/main` est vide, aucun conflit possible.
2. Avant : `Post-Uni` commitée et poussée, et le tag `alpha` sur GitHub (c'est le cas).
3. Commandes, lancées par Paul :
   ```bash
   git checkout main
   git merge --ff-only Post-Uni
   git push origin main
   ```
4. Le push déclenche le redéploiement Render de `riftlog-backend` : **merger, c'est mettre en prod**. Au démarrage, Flyway applique V3 (suppression des tables v1, schéma v2) et V4 à la base `production`, puis les cartes sont importées depuis Riftcodex (1 à 2 minutes).
5. Après : régler Render et créer le site, puis tout vérifier (checklist dans [DEPLOY.md](DEPLOY.md)).

**En ligne depuis le 2026-11-26** : site https://riftlog.onrender.com, API https://riftlog-backend.onrender.com. Parcours complet vérifié (inscription, appli installable, match en ligne et hors-ligne, historique, stats).

**Branches Neon** (renommées le 2026-11-26) : le serveur de prod avait été branché sur la branche `v2`, qui avait déjà le schéma v2 et les cartes. On l'a gardée comme base de prod plutôt que de migrer l'ancienne :

| Branche Neon | id | Contenu |
| --- | --- | --- |
| `production` (par défaut) | `br-old-feather-za30tzqi` | **base de prod v2**, ex-`v2` (hôte `ep-plain-feather-…`) |
| `v1-archive` | `br-broad-tooth-zawy9qy4` | ex-`production`, base v1 jamais migrée (hôte `ep-cold-rain-…`) |
| `v1-backup` | `br-fragrant-flower-zakz1igo` | copie de `v1-archive`, sans compute |

**Base de données (Flyway)**

6. **On ne modifie jamais un fichier de migration déjà appliqué** (`V1__…` à `V4__…`) : Flyway refuserait de démarrer.
7. Tout changement de schéma passe par un **nouveau fichier** `V5__…`, `V6__…`, testé d'abord sur une nouvelle branche Neon créée depuis `production` (console Neon → Create branch), jamais directement sur `production`. Même règle pour le développement local : ne pas brancher son backend local sur `production`.

**Sauvegarde et retour arrière**

8. La branche Neon **`v1-backup`** (`br-fragrant-flower-zakz1igo`) est une copie de la prod v1 prise juste avant le merge (schéma V1/V2, 3 comptes de test). Elle n'a pas de compute : en ajouter un depuis la console Neon pour la consulter.
9. Retour arrière vers la v1 en cas de gros problème : redéployer sur Render le commit du tag `alpha` avec un `DB_URL` pointant sur `v1-archive`. Ne pas faire tourner le code v2 sur une base v1, ni l'inverse.
10. `v1-archive` et `v1-backup` contiennent la même chose : on pourra supprimer l'une des deux plus tard.

**Ensuite**

11. Tant que personne d'autre n'utilise l'appli, on peut travailler directement sur `main`.
12. Dès que les amis enregistrent leurs parties, chaque push sur `main` touche leurs données : on reprend une branche de travail (`dev`) et on ne merge dans `main` que ce qui est testé.

## 8. Questions ouvertes

- ~~Préprod séparée ou merge direct ?~~ Merge direct (voir §7 bis).
- ~~La base Neon de prod contient-elle des données à garder ?~~ Non (vérifié le 2026-11-26) : seulement des données de test (Alice/Bob, légendes fictives Ada/Jiro/Kaya, 10 matchs). On repart d'un schéma propre.
- DA PC « joli » : lien ou capture de Papertree.
- ~~Option pour ne pas télécharger les données ?~~ Inutile : tout ce qui est gardé sur l'appareil pèse environ 330 Ko (appli, polices et icônes) plus 37 Ko de données de jeu (7 Ko compressés). Aucune image de carte n'est téléchargée.
- L'hébergement : le plan gratuit de Render met le serveur en veille, ce qui ralentit le premier envoi.
- Riftatlas : API, conditions d'utilisation, comment fait Riftlite.
