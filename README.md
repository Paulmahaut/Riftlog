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

  1. Prerequisites: Java + Maven (or just the `mvnw` wrapper, no separate install needed), Node/npm,
     Docker + Docker Compose.

  2. Start Postgres (from the repo root):
     ```
     docker compose up -d
     ```
     Starts Postgres on `localhost:5432` (db/user/password: `riftlog`, see `docker-compose.yml`).

  3. Launch the backend (from `backend/`):
     ```
     ./mvnw spring-boot:run
     ```
     Opens the API on `http://localhost:8080`. Flyway creates the schema automatically on startup,
     but there is no seeded account: you must register a real user before the API will do anything
     else, since every match/deck is owned by whoever is authenticated. Either through the
     frontend's register screen once it exists, or directly:

    **use precisely this command**
     ```
     curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"email":"you@example.com","password":"changeme123","displayName":"Ready Player One"}'

     ```
     This returns a JWT `token` — send it as `Authorization: Bearer <token>` on every other
     `/api/**` call (everything except `/api/auth/register` and `/api/auth/login` requires it; the
     API is stateless, there is no server-side session/cookie).

  4. Launch the frontend (from `frontend/`):
     ```
     npm install
     npm run dev
     ```
     Opens the app on `http://localhost:5173`.

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
