# Architecture

How a request flows through Riftlog, and why the code is split the way it is.

Request flow

  Controller → Service → Repository → Entity → Database.
  Each layer only talks to the one below it (e.g. Controllers never touch Repositories directly).

Repo layout

  backend/     the Maven project (pom.xml, mvnw, src/) — everything below is relative to here.
  frontend/    reserved for the mobile web client (not started yet).

Package layout (`com.riftlog`, under `backend/src/main/java`)

  entity/      JPA-mapped classes, one per table (Legend, Player, Deck, Match, MatchRound, Result).

  repository/  Spring Data JPA interfaces — no SQL written by hand, method names are enough
               (e.g. findByNameIgnoreCase).

  dto/         Request/response records — the API's public shape. Kept separate from entity/
               so the storage model can change without breaking API consumers.

  service/     Business rules (e.g. a match ends when one side reaches 8 points, results are
               derived from scores, names are reused instead of duplicated).

  controller/  REST endpoints. Thin — receive, validate (@Valid), delegate to a service.

  exception/   Custom exceptions + one @ControllerAdvice turning them into consistent JSON errors.

Data model

  Legend --< Deck --< Match >-- Player
                        |
                        ^
                    MatchRound (per-round cumulative score)

  A Match links two Decks (mine / opponent's) and one Player (the opponent). Its MatchRounds
  record the running score after each round, so per-round analysis stays possible later even
  though the Match itself (final result) is the primary unit.

Key rule

  A match is only valid once one side's score reaches 8 in its last round — enforced in
  MatchService, not in the database.

Schema changes

  Flyway owns the schema, not Hibernate: `backend/src/main/resources/db/migration/V1__init_schema.sql`
  is the source of truth, and Hibernate only checks the DB matches the entities (ddl-auto=validate) —
  it never modifies anything itself. Changing an entity means writing the matching SQL in a new
  `V2__...sql`, `V3__...sql`, etc. Existing (already-applied) migration files are never edited.

API surface

  POST /api/matches        log a full match + its rounds in one call (the README's "RPC" use case)
  GET  /api/matches        list history, optional ?opponentId= / ?deckId= filters
  GET  /api/matches/{id}   one match with its rounds
  GET  /api/stats          win rate: overall, by deck, by matchup
  GET/POST /api/decks      lookups + creation (create-or-reuse by name)
  GET/POST /api/legends    same, for legends

Not built yet (by design, see README for the eventual scope)

  Auth / multi-user, Postgres in prod, deck versioning, matchup analytics beyond win rate,
  the mobile web client itself.
