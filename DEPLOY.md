# Deploying a shared instance

## Préprod v2 (branche `Post-Uni`)

Pour tester la v2 en ligne **sans toucher la prod** : un backend et un site Render construits depuis
`Post-Uni`, branchés sur la branche Neon `v2`. `main`, la prod Render et la base `production` ne bougent pas.
Render reconstruit la préprod à chaque push sur `Post-Uni`.

**Avant** : commiter et pousser `Post-Uni` (Render construit ce qui est sur GitHub, pas ce qui est en local).

### 1. Backend de préprod (Render → New → Web Service)

| Réglage | Valeur |
| --- | --- |
| Repo / branche | `Riftlog` / `Post-Uni` |
| Name | `riftlog-backend-v2` |
| Language | Docker |
| Root Directory | `backend` |
| Dockerfile Path | `./Dockerfile` |
| Instance type | Free |
| Health Check Path (Advanced) | `/validation.html` |

Variables d'environnement :

| Clé | Valeur |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<host de la branche v2>/neondb?sslmode=require&channel_binding=require` |
| `DB_USERNAME` / `DB_PASSWORD` | ceux de la branche Neon `v2` (Neon → Branches → `v2` → Connect) |
| `JWT_SECRET` | une nouvelle chaîne aléatoire de 40+ caractères (bouton « Generate » de Render) |
| `CORS_ALLOWED_ORIGINS` | l'URL du site de l'étape 2 (à remplir après l'avoir créé) |

Vérifier : `https://riftlog-backend-v2.onrender.com/validation.html` répond (premier build : quelques minutes).

### 2. Site de préprod (Render → New → Static Site)

| Réglage | Valeur |
| --- | --- |
| Repo / branche | `Riftlog` / `Post-Uni` |
| Name | `riftlog-v2` |
| Root Directory | `frontend` |
| Build Command | `npm ci && npm run build` |
| Publish Directory | `dist` |
| Variable `VITE_API_URL` | `https://riftlog-backend-v2.onrender.com` |
| Variable `NODE_VERSION` | `22` |

Puis **Redirects/Rewrites** : Source `/*`, Destination `/index.html`, Action **Rewrite**
(sinon recharger `/stats` ou `/matches/…` donne une 404).

Enfin, revenir au backend et mettre `CORS_ALLOWED_ORIGINS=https://riftlog-v2.onrender.com`.

### 3. Vérifier

1. Ouvrir `https://riftlog-v2.onrender.com` sur le téléphone. Le premier appel peut prendre ~1 min (réveil du serveur gratuit).
2. Se connecter avec un compte créé sur `v2`, ou en créer un.
3. « Ajouter à l'écran d'accueil », puis couper le réseau et rouvrir l'appli : elle doit s'ouvrir.
4. Jouer un match hors-ligne, remettre le réseau : le badge passe à « À jour », le match apparaît dans l'historique et les stats.

Quand la v2 passera en prod (merge dans `main`), même recette sur le service `riftlog-backend` existant
(base `production`, `CORS_ALLOWED_ORIGINS` du site de prod) et un site statique construit depuis `main`.

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
