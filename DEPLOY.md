# Deploying a shared instance

## Mettre la v2 en prod (merge `Post-Uni` → `main`)

Décision et retour arrière : voir `SPEC-v2.md` §7 bis.

**Fait le 2026-11-26.** Site : https://riftlog.onrender.com · API : https://riftlog-backend.onrender.com.
La base de prod est la branche Neon `production` (ex-`v2`, hôte `ep-plain-feather-…`). L'ancienne base v1 est
`v1-archive` (hôte `ep-cold-rain-…`), sauvegardée aussi dans `v1-backup`. Le tableau des branches est dans la SPEC.
La procédure ci-dessous reste la référence pour recréer un service.

Il y a deux services Render :

- **`riftlog-backend`** (Web Service, existe déjà) : il se redéploie tout seul au push sur `main` ;
- **le site** (Static Site, à créer) : le nouveau frontend Vue.

L'adresse `xxx.onrender.com` d'un service est fixée à sa création : copier toujours celle affichée en haut
de sa page, pas son nom. La région d'un Web Service ne peut plus changer après création.

### 1. Avant le merge : vérifier `riftlog-backend`

| Où dans Render | Champ | Valeur attendue |
| --- | --- | --- |
| Settings | Branch | `main` |
| Settings | Root Directory | `backend` |
| Settings | Dockerfile Path | `./Dockerfile` |
| Settings | Health Check Path | `/validation.html` |
| Environment | `DB_URL` | branche Neon **`production`** : `jdbc:postgresql://ep-plain-feather-zae35cni-pooler.c-2.eu-west-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require` |
| Environment | `DB_USERNAME` / `DB_PASSWORD` | `neondb_owner` / mot de passe affiché dans Neon (branche `production` → Connect) |
| Environment | `JWT_SECRET` | déjà défini (32 caractères ou plus), ne pas changer : sinon tout le monde est déconnecté |
| Environment | `CORS_ALLOWED_ORIGINS` | sera mis à jour à l'étape 4 |

À ne pas définir : `PORT` (fourni par Render), `JWT_EXPIRATION_MS` (30 jours par défaut),
`CARDS_IMPORT_ON_STARTUP` (vrai par défaut : la base de prod n'a pas encore les cartes v2).

### 2. Le merge

```bash
git checkout main
git merge --ff-only Post-Uni
git push origin main
```

Dans les logs du déploiement Render (`riftlog-backend` → Logs), on doit voir :

- `Successfully applied 2 migrations … now at version v4` ;
- `Card import done: 1451 fetched … 49 legends, 64 battlefields` (1 à 2 minutes après le démarrage).

### 3. Créer le site (Render → New → Static Site)

| Où dans Render | Champ | Valeur | Erreur à éviter |
| --- | --- | --- | --- |
| Source | Branch | **`main`** | — |
| Général | Root Directory | **`frontend`** | vide : `npm` ne trouve pas `package.json` |
| Général | Build Command | **`npm ci && npm run build`** | — |
| Général | Publish Directory | **`dist`** (préfixe `frontend/` affiché) | `build` ou `public` : page blanche |
| Environment | `VITE_API_URL` | `https://<adresse-du-backend>.onrender.com` | slash final : les appels deviennent `//api/…` et sont refusés |
| Environment | `NODE_VERSION` | `22` | trop ancien : le build échoue (Vite 8) |
| Redirects/Rewrites | Règle | Source `/*`, Destination `/index.html`, Action **Rewrite** | absente : recharger `/stats` donne une 404 |

`VITE_API_URL` est lue au moment du build : si on la change, il faut redéployer le site.

### 4. Autoriser le site côté backend

`riftlog-backend` → Environment → `CORS_ALLOWED_ORIGINS` = `https://<adresse-du-site>.onrender.com`
(en `https`, **sans slash final**, sans chemin), puis **Save, rebuild and deploy**.
On peut ajouter `,http://localhost:5173` pour développer en local contre la prod.

### 5. Vérifier

1. `https://<adresse-du-backend>/validation.html` répond.
2. Sur le site : créer un compte. Le badge passe à « À jour » et l'écran Jouer propose les légendes.
3. Recharger une page comme `/stats` : pas de 404.
4. Sur le téléphone : « Ajouter à l'écran d'accueil », mode avion, rouvrir l'appli : elle s'ouvre.
5. Jouer un match hors-ligne, remettre le réseau : le match part et apparaît dans l'historique et les stats.

Le premier appel après 15 minutes d'inactivité prend environ 1 minute : le serveur gratuit se réveille.

### 6. Ensuite

- Supprimer les éventuels services de préprod créés avant cette décision.
- Garder `v1-archive` et `v1-backup` (même contenu : l'une des deux pourra être supprimée).

---

*La suite de ce fichier décrit la mise en place initiale (v1) et reste valable pour Neon et Render.*


For a dozen people to hit the same data, one backend + one database need to run
somewhere everyone can reach — not on your laptop. This uses two free
services, no credit card required for either:

- **[Neon](https://neon.tech)** — the Postgres database. Free tier: 0.5 GB
  storage, never expires, no card needed. It suspends after 5 minutes of
  inactivity and wakes up automatically on the next query (a second or two of
  delay, not a problem).
- **[Render](https://render.com)** — runs the Spring Boot API as a Docker
  container (`backend/Dockerfile`). Free tier: 750 instance-hours/month, no
  card needed. It spins down after 15 minutes of inactivity and takes about a
  minute to wake up on the next request — the first person to open the app
  after a quiet period just waits a bit.

For ~12 casual users this is a reasonable trade: fully free, versus paying
~$5-7/month somewhere (e.g. Railway) to avoid the cold starts.

## 1. Create the database (Neon)

The `Riftlog` project already exists on Neon (org `paul.mahaut@gmail.com`,
project id `wispy-star-87521131`, branch `production`). To get its
credentials on a new machine, either:

- **Neon CLI** (what was used to set this up): `npm i -g neon`, `neon login`,
  then `neon link --project-id wispy-star-87521131 --branch production` from
  the repo root. This writes `.env.local` with `DATABASE_URL` (gitignored —
  never commit it). Split it for step 2 below:
  - `DB_URL` = `jdbc:postgresql://<host-from-DATABASE_URL>/<database>?channel_binding=require&sslmode=require`
  - `DB_USERNAME` / `DB_PASSWORD` = the `user`/`password` part of `DATABASE_URL`
- **Dashboard**: [neon.tech](https://neon.tech) → the project → copy the
  connection string shown there, same split as above.

Nothing else to do — Flyway creates the schema automatically the first time
the backend starts against it (already verified working end to end).

> **Heads-up on `neon mcp`**: if you also run the CLI's `mcp`/`skills` setup
> (adds Neon tooling to Claude Code), it mints an API key that "reaches
> everything your account can, in every organization" and stores it in your
> *global* `~/.claude.json`, not just this repo. Fine for a solo dev machine;
> revoke it with `neon api-keys revoke <id>` (shown when it's minted) if you'd
> rather not keep it around.

## 2. Deploy the API (Render)

1. Sign up at [render.com](https://render.com) and connect your GitHub account.
2. **New > Blueprint**, pick this repo. Render reads `render.yaml` at the repo
   root and finds the `riftlog-backend` service automatically.
3. It will ask you to fill in the env vars marked `sync: false`:
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — from step 1
   - `JWT_SECRET` — a long random string, **not** the dev default in
     `application.properties`. Generate one with:
     ```
     openssl rand -base64 48
     ```
     (no openssl handy? any random 40+ character string works)
   - `CORS_ALLOWED_ORIGINS` — the origin(s) the frontend will be served from,
     comma-separated if more than one (e.g.
     `http://localhost:5173,https://riftlog.example.com`). Update this once
     the frontend has its own deployed URL.
4. Deploy. First build takes a few minutes (Maven downloads dependencies
   inside the container, same as running `mvnw` locally the first time).
5. Render gives you a public URL like `https://riftlog-backend.onrender.com`.
   Sanity check it:
   ```
   curl -X POST https://riftlog-backend.onrender.com/api/auth/register \
     -H "Content-Type: application/json" \
     -d "{\"email\":\"you@example.com\",\"password\":\"changeme123\",\"displayName\":\"You\"}"
   ```
   A JSON response with a `token` means the database connection and the API
   are both working end to end.

## 3. Point clients at it

Once deployed, every client — the web frontend, and later the Android app —
should call `https://riftlog-backend.onrender.com` instead of
`http://localhost:8080`. There's nothing shared beyond that URL: each device
just needs it plus, per user, the JWT they get back from login.

## Updating the schema later

Same rule as local dev: new columns/tables go in a new
`backend/src/main/resources/db/migration/Vn__*.sql` file. Render rebuilds and
redeploys automatically on every push to the connected branch, and Flyway
applies any new migration against Neon on startup — no manual DB step needed.
