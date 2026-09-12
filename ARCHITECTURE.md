# Architecture

How a request flows through Riftlog, and why the code is split the way it is.

Request flow

  Controller → Service → Repository → Entity → Database.
  Each layer only talks to the one below it (e.g. Controllers never touch Repositories directly).

Repo layout

  backend/     the Maven project (pom.xml, mvnw, src/) — everything below is relative to here.
  frontend/    the Vue 3 / Vite web client (routing scaffolded; wiring to the API is in progress).
  docker-compose.yml   local Postgres for dev — `docker compose up -d` before running the backend.

Package layout (`com.riftlog`, under `backend/src/main/java`)

  entity/      JPA-mapped classes, one per table (User, Legend, Player, Deck, Match, MatchRound, Result).

  repository/  Spring Data JPA interfaces — no SQL written by hand, method names are enough
               (e.g. findByNameIgnoreCase).

  dto/         Request/response records — the API's public shape. Kept separate from entity/
               so the storage model can change without breaking API consumers.

  service/     Business rules (e.g. a match ends when one side reaches 8 points, results are
               derived from scores, names are reused instead of duplicated).

  controller/  REST endpoints. Thin — receive, validate (@Valid), delegate to a service.

  security/    JWT issuing/parsing (JwtService), the per-request auth filter
               (JwtAuthenticationFilter), and the Spring Security filter chain (SecurityConfig).
               Stateless: no HttpSession, no server-side login state — every request carries its
               own bearer token.

  exception/   Custom exceptions + one @ControllerAdvice turning them into consistent JSON errors.

Data model

  User --< Deck --< Match >-- Player
   |                  |
   ^                  ^
   (owns matches)  MatchRound (per-round cumulative score)

  Every Deck and Match belongs to a User (owner_id) — a personal log, not a shared one. A Match
  links two Decks (mine / opponent's, both owned by the logged-in user) and one Player (the
  opponent, a freeform name — not a registered account). Its MatchRounds record the running
  score after each round, so per-round analysis stays possible later even though the Match
  itself (final result) is the primary unit. Legends are the one shared/global reference table.

Key rule

  A match is only valid once one side's score reaches 8 in its last round — enforced in
  MatchService, not in the database.

Auth

  Registration/login issue a JWT (HS256, `app.jwt.secret`/`app.jwt.expiration-ms`) carrying the
  user's id/email/displayName as claims. JwtAuthenticationFilter reads the `Authorization: Bearer
  <token>` header on every request and populates the Spring Security context from it — no DB
  lookup, no session, so the service stays stateless. Every `/api/**` route requires a valid
  token except `/api/auth/register` and `/api/auth/login`. Controllers pull the current user via
  `@AuthenticationPrincipal AuthenticatedUser`.

Schema changes

  Flyway owns the schema, not Hibernate: `backend/src/main/resources/db/migration/V1__init_schema.sql`
  is the source of truth, and Hibernate only checks the DB matches the entities (ddl-auto=validate) —
  it never modifies anything itself. Changing an entity means writing the matching SQL in a new
  `V2__...sql`, `V3__...sql`, etc. Existing (already-applied) migration files are never edited —
  V1/V2 were the one exception, rewritten in place to add `users` and `owner_id` while Postgres
  was still unwired and no real data existed anywhere yet; from here on, new columns/tables go in
  a new migration file.

API surface

  POST /api/auth/register  create an account (email + password + displayName), returns a JWT
  POST /api/auth/login     returns a JWT for existing credentials
  GET  /api/auth/me        the current user, resolved from the bearer token
  POST /api/matches        log a full match + its rounds in one call (the README's "RPC" use case)
  GET  /api/matches        list the current user's history, optional ?opponentId= / ?deckId= filters
  GET  /api/matches/{id}   one of the current user's matches with its rounds (404 if it's someone else's)
  GET  /api/stats          the current user's win rate: overall, by deck, by matchup
  GET/POST /api/decks      the current user's decks — lookups + creation (create-or-reuse by name)
  GET/POST /api/legends    shared across all users, same create-or-reuse behaviour

  All routes above except /api/auth/register and /api/auth/login require
  `Authorization: Bearer <token>`.

Not built yet (by design, see README for the eventual scope)

  Frontend login/register screens and API wiring (owned by the other half of the pair), deck
  versioning, matchup analytics beyond win rate. Known simplification: opponents (the `players`
  table) are a shared name pool, not scoped per-user — two different accounts logging a match
  against someone named "Bob" share one Player row. Harmless today (no private data on that
  table) but worth revisiting if opponent-side features grow.
