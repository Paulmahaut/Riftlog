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

  Windows (PowerShell; `winget` ships with Windows 10/11):
     ```
     winget install --id Git.Git -e
     winget install --id EclipseAdoptium.Temurin.21.JDK -e
     winget install --id OpenJS.NodeJS.LTS -e
     winget install --id Docker.DockerDesktop -e
     ```
     Docker Desktop needs WSL2 (`wsl --install` if it asks) and, after install, must be **launched
     once** (the app, not just installed) before `docker compose` works.

  macOS ([Homebrew](https://brew.sh)):
     ```
     brew install git openjdk@21 node
     brew install --cask docker
     sudo ln -sfn "$(brew --prefix openjdk@21)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-21.jdk
     ```
     Then launch the Docker Desktop app once before `docker compose` works.

  Linux (Ubuntu/Debian; other distros — same idea via your package manager):
     ```
     sudo apt update && sudo apt install -y git openjdk-21-jdk ca-certificates curl
     curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash - && sudo apt install -y nodejs

     # Docker Engine + Compose plugin, from Docker's own repo (Ubuntu's own packages vary by version)
     sudo install -m 0755 -d /etc/apt/keyrings
     curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
     echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
     sudo apt update && sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
     sudo usermod -aG docker $USER
     ```
     Log out/in once for the `docker` group membership to take effect.

  Verify any OS: `git --version && java -version && node -v && docker compose version`

  1. Start Postgres (from the repo root, any OS):
     ```
     docker compose up -d
     ```
     Starts Postgres on `localhost:5432` (db/user/password: `riftlog`, see `docker-compose.yml`).
     Docker Desktop must actually be running first (not just installed).

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

  Done
    Backend MVP (entities, services, REST/RPC endpoints, exception handling) — see ARCHITECTURE.md.
    Unit test coverage for the service layer.
    Flyway migrations (schema is versioned, no more auto-guessed DDL).
    Multi-user auth (JWT) — matches/decks are now private per registered user.
    PostgreSQL everywhere (Docker locally, Neon in prod) — no more H2.
    Frontend brief handed off (FRONTEND.md) and a real Vue frontend started (frontend/).
    Deployment path documented and working (DEPLOY.md — Render + Neon, both free tier).

  Next
    Finish the frontend screens (log match, history, stats) per FRONTEND.md, using the shared
    demo account (frontend/src/api.js) — no login UI needed for the prototype, see Scope below.
    Try a real end-to-end duel: log a match, check stats, from the actual UI.

  Later
    A real login/register screen and per-user accounts in the UI (the backend already supports
    this — see Scope below).
    Richer stats (going-first split, per-round analysis, deck versioning) — see RiftLite for inspiration.
    Android app (native or wrapped web client) consuming this API.

Scope: coursework prototype vs. the long-term plan

  This started as a school project but is meant to keep going afterwards as a real personal app
  (see the original goal in earlier design notes: usable by others, eventually via an Android app).
  That's why the backend already has full multi-user auth (JWT, each account's data private) even
  though it's more than the coursework prototype strictly needs.

  For the version being handed in, the frontend deliberately skips building a login/register UI:
  it logs in automatically as one single shared demo account (frontend/src/api.js) and every screen
  just uses that. This keeps the UI scope to the actual gameplay features (log a match, history,
  stats) instead of account management, without throwing away the auth work — turning it into real
  multi-account usage later is a frontend-only change (a login screen + storing which user is
  active), the backend needs nothing new.
