# Deploying a shared instance

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
