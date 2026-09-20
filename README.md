# Riftlog

RiftIRL — Spring Boot backend service and mobile-first web client for tracking tabletop match history (TCG/Riftbound).

Stack & Architecture

  Backend: Java / Spring Boot, layered architecture (Controller, Service, Entity).

  Persistence: Spring Data JPA / PostgreSQL (Flyway-managed schema, run locally via `docker-compose.yml`).

  Auth: JWT, stateless (no server-side session) — each registered user's matches and decks are private to them.

  API: REST (stats, match history, match-up filters) and RPC (fast end-of-game logging).

  Core: Dependency Injection (@Autowired), global HTTP exception handling (@ControllerAdvice), application logging (SLF4J).

  See `ARCHITECTURE.md` for how requests flow through the code, `FRONTEND.md` for the frontend brief,
  and `DEPLOY.md` for deploying a shared instance.

Running it locally

  Prerequisites — install once per machine, skip anything already present. Needed either way:
  JDK 21, Node.js `^20.19.0` or `>=22.12.0` (Vite 8's requirement), Docker + Docker Compose.
  Maven itself isn't needed — `mvnw`/`mvnw.cmd` (checked into `backend/`) downloads it on first run.

  <details>
  <summary>Install on Windows (winget, ships with Windows 10/11)</summary>

  ```
  winget install --id Git.Git -e
  winget install --id EclipseAdoptium.Temurin.21.JDK -e
  winget install --id OpenJS.NodeJS.LTS -e
  winget install --id Docker.DockerDesktop -e
  ```
  Needs WSL2 (`wsl --install` if it asks). Docker Desktop must be **launched once** (the app, not
  just installed) before `docker compose` works.
  </details>

  <details>
  <summary>Install on macOS (Homebrew)</summary>

  ```
  brew install git openjdk@21 node
  brew install --cask docker
  sudo ln -sfn "$(brew --prefix openjdk@21)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-21.jdk
  ```
  Launch the Docker Desktop app once before `docker compose` works.
  </details>

  <details>
  <summary>Install on Linux (Ubuntu/Debian; other distros — same idea, own package manager)</summary>

  ```
  sudo apt update && sudo apt install -y git openjdk-21-jdk ca-certificates curl
  curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash - && sudo apt install -y nodejs

  # Docker Engine + Compose plugin from Docker's own repo (Ubuntu's own packages vary by version)
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt update && sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
  sudo usermod -aG docker $USER   # log out/in after, for the group to take effect
  ```
  </details>

  Verify any OS: `git --version && java -version && node -v && docker compose version`

  1. Database — pick one:

     **Local Postgres (default, isolated per machine):**
     ```
     docker compose up -d
     ```
     `localhost:5432`, db/user/password `riftlog` (see `docker-compose.yml`). Docker Desktop must
     actually be running first, not just installed.

     <details><summary>Or: the shared Neon branch instead (same data for both devs, no local Docker)</summary>

     ```
     npm i -g neon
     neon login
     neon link --project-id wispy-star-87521131 --branch production   # once per machine
     neon checkout dev                                                  # creates/switches to a shared "dev" branch
     ```
     `neon checkout` writes `.env.local` with a `DATABASE_URL` — split it into three env vars
     (same values, just re-shaped) before step 2 below:
     ```
     DB_URL=jdbc:postgresql://<host>/<database>?channel_binding=require&sslmode=require
     DB_USERNAME=<user>
     DB_PASSWORD=<password>
     ```
     Use a `dev` branch, not `production` — that one is what Render actually serves. Full Neon
     setup (incl. a security note on `neon mcp`) is in DEPLOY.md.
     </details>

  2. Launch the backend (from `backend/`):
     ```
     ./mvnw spring-boot:run          # macOS / Linux / Git Bash
     mvnw.cmd spring-boot:run        # Windows (cmd or PowerShell)
     ```
     Opens the API on `http://localhost:8080`. Flyway creates the schema automatically on startup,
     but there is no seeded account — you must register one before the API does anything else,
     since every match/deck is owned by whoever is authenticated.

  3. **One-time per database**: create the shared demo account the frontend logs in as
     automatically (`frontend/src/api.js` — no login UI in this prototype, see Scope below).
     Skip this only if you're pointing at a database where it already exists:
     ```
     curl -X POST http://localhost:8080/api/auth/register \
       -H "Content-Type: application/json" \
       -d "{\"email\":\"demo@riftlog.local\",\"password\":\"riftlog-demo-2026\",\"displayName\":\"Demo\"}"
     ```
     PowerShell equivalent:
     ```
     Invoke-RestMethod -Uri http://localhost:8080/api/auth/register -Method Post `
       -ContentType "application/json" `
       -Body '{"email":"demo@riftlog.local","password":"riftlog-demo-2026","displayName":"Demo"}'
     ```
     A `400`/`EmailAlreadyUsedException` response just means it's already registered — fine,
     move on. Without this step the frontend's login call 401s forever (it retries the same
     hardcoded credentials on every request, there's no error message pointing back here).

  4. Launch the frontend (from `frontend/`, in a second terminal — same commands on every OS):
     ```
     npm install
     npm run dev
     ```
     Opens the app on `http://localhost:5173`, already wired to the backend through Vite's dev
     proxy (`/api/**` → `http://localhost:8080`, see `vite.config.js`) — no extra config needed.

  To register a different, real (non-demo) account instead — e.g. to test the multi-user auth
  path directly — reuse the same `curl`/`Invoke-RestMethod` call above with your own email/password.
  Either way this returns a JWT `token` — send it as `Authorization: Bearer <token>` on every other
  `/api/**` call (everything except `/api/auth/register` and `/api/auth/login` requires it; the
  API is stateless, there is no server-side session/cookie).

Roadmap

  Done: backend MVP (entities, services, REST/RPC, exception handling — see ARCHITECTURE.md),
  service-layer unit tests, Flyway migrations, JWT multi-user auth, Postgres everywhere (Docker
  locally, Neon in prod), a real deploy path (DEPLOY.md), and a Vue frontend started (frontend/).

  Next: finish the remaining frontend screens (Play, Decks, Stats — see FRONTEND.md) and run one
  real match through the actual UI end to end.

  Later: a real login/register screen (the backend already supports it, see Scope below), richer
  stats (going-first split, per-round analysis, deck versioning), an Android client.

Scope: coursework prototype vs. the long-term plan

  Started as a school project, meant to keep going afterwards as a real personal app — that's why
  the backend already has full multi-user auth even though the prototype doesn't strictly need it.

  The frontend deliberately has no login/register screen yet: every screen logs in automatically
  as one shared demo account (`frontend/src/api.js`), keeping the UI scope to gameplay (log a
  match, history, stats) instead of account management. Adding real per-user login later is a
  frontend-only change — a login screen plus tracking which user is active — the backend needs
  nothing new.
