# Riftlog

RiftIRL — Spring Boot backend service and mobile-first web client for tracking tabletop match history (TCG/Riftbound).

Stack & Architecture

  Backend: Java / Spring Boot, layered architecture (Controller, Service, Entity).

  Persistence: Spring Data JPA / PostgreSQL (Flyway-managed schema, run locally via `docker-compose.yml`).

  Auth: JWT, stateless (no server-side session) — each registered user's matches and decks are private to them.

  API: REST (stats, match history, match-up filters) and RPC (fast end-of-game logging).

  Core: Dependency Injection (@Autowired), global HTTP exception handling (@ControllerAdvice), application logging (SLF4J).

See `LAUNCH.md` to run it locally and `ARCHITECTURE.md` for how requests flow through the code.
