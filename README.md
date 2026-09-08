# Riftlog

RiftIRL — Spring Boot backend service and mobile-first web client for tracking tabletop match history (TCG/Riftbound).

Stack & Architecture

  Backend: Java / Spring Boot, layered architecture (Controller, Service, Entity).

  Persistence: Spring Data JPA / Relational database.

  API: REST (stats, match history, match-up filters) and RPC (fast end-of-game logging).

  Core: Dependency Injection (@Autowired), global HTTP exception handling (@ControllerAdvice), application logging (SLF4J).
