# Riftlog

**CardQuest** — Mobile-first web application for tracking **Riftbound TCG** match history.

Riftlog lets players record their matches, decks, rounds, and results, then review their match history and statistics.

---

## Stack

### Backend

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT Authentication
- REST API
- SLF4J

### Frontend

- Vue 3
- Vite
- Tailwind CSS

### Database

- PostgreSQL
- [Neon](https://neon.tech/) for shared development and production
- Docker PostgreSQL available for isolated local development

---

## Architecture

```text
┌─────────────────────┐
│     Vue Frontend    │
│    localhost:5173   │
└──────────┬──────────┘
           │
           │ /api/*
           ▼
┌─────────────────────┐
│    Spring Boot      │
│    localhost:8080   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│    PostgreSQL       │
│        Neon         │
└─────────────────────┘
```

The backend follows a layered architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

See also:

- `ARCHITECTURE.md` — backend architecture and request flow
- `FRONTEND.md` — frontend structure and requirements
- `DEPLOY.md` — deployment instructions

---

<br>
<br>
<br>
<br>
<br>
<br>

# Installation

## Prerequisites

Install the following once per machine:

- JDK 21
- Node.js `^20.19.0` or `>=22.12.0`
- Git
- Docker + Docker Compose only if you want to use a local PostgreSQL database (whch is an old fall back - or used just to dev in local)

Maven itself is not required. The project includes `mvnw` / `mvnw.cmd`.

### Verify your installation

```bash
git --version
java -version
node -v
docker compose version
```

---

# Database

_Note that the `Docker volume` is only used as a fallback system to keep the project alive._

## Neon (the final one - the one being used)

This is the recommended configuration for shared development and production.

The backend uses the following environment variables:

```env
DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require&channel_binding=require
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

Example:

```env
DB_URL=jdbc:postgresql://ep-example.eu-west-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require
DB_USERNAME=neondb_owner
DB_PASSWORD=your-password
```

Flyway automatically creates and updates the database schema when the backend starts.

## Local PostgreSQL with Docker

```bash
docker compose up -d
```

The database will be available at:

```text
Host: localhost
Port: 5432
Database: riftlog
Username: riftlog
Password: riftlog
```

This database is completely independent from Neon. And only work as a fallback.

<br><br><br>

# Environment Configuration

The backend expects the following environment variables:

```env
DB_URL=jdbc:postgresql://...
DB_USERNAME=...
DB_PASSWORD=...
```

The `.env` file should be located at the root of the project with the `.vscode/launch.json`:

```text
Riftlog/
├── .env
.vscode/
└── launch.json
```

The, `.vscode/launch.json` is configured to automatically load `.env` when starting the application through **Run & Debug** or **Run**.

<br><br><br><br>

# Running the Project - Quick start

From the `backend/` directory:

```bash
./mvnw spring-boot:run
```
<br>

BUT this commmand doesn't work all the time depending on your current computer config. <br>

Prefer using this : <br>
From the `backend/` directory:
```bash
./mvnw clean compile
```
```bash
./mvnw dependency:resolve
```
> windows + shift + P --> **Java: Clean Java Language Server Workspace**

wait till vscode restart, then simply click the vscode buttons to **Run & Debug**

<br>

From the `frontend/` directory:

```bash
npm install
npm run dev
```

Now you should be able to see the project on

> http://localhost:5173/

<br><br><br><br><br>

---

# Authentication

Riftlog uses **stateless JWT authentication**.

There are no server-side sessions or authentication cookies.

After logging in, the API returns a JWT.

Protected endpoints must receive the token using:

```http
Authorization: Bearer <token>
```

The following endpoints do not require authentication:

```text
POST /api/auth/register
POST /api/auth/login
```

All other `/api/**` endpoints require a valid JWT.

---

## Creating an Account - (only if you are using Docker volumes fallback)

The frontend does not currently include a login or registration screen.

An account can be created directly through the API:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"changeme123","displayName":"Ready Player One"}'
```

The response contains a JWT.

If the email is already registered, the API will return an error indicating that the account already exists.

---

## Logging In - (only if you are using Docker volumes fallback)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"changeme123"}'
```

The response contains the JWT:

```json
{
    "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

Use this token for authenticated requests.

---

## Testing the API - (only if you are using Docker volumes fallback)

### Get the authenticated user

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

### Get match history

```bash
curl http://localhost:8080/api/matches \
  -H "Authorization: Bearer <TOKEN>"
```

Matches are private. Only matches belonging to the authenticated user are returned.

### Get statistics

```bash
curl http://localhost:8080/api/stats \
  -H "Authorization: Bearer <TOKEN>"
```

---

<br><br><br><br><br><br><br><br>

# ⚠️⚠️ Git & Secrets | READ THIS ⚠️⚠️

The repository is ignoring files containing secrets:

```gitignore
.env
.env.*
!.env.example
```

The `.env.example` file included the required variables to connect to the DB - and so, to fully run the project:

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
```

---

# Main API Endpoints

| Method   | Endpoint                  | Description |
| -------- | ------------------------- | ----------- |
| `POST`   | `/api/auth/register`      | Create an account (no token needed) |
| `POST`   | `/api/auth/login`         | Get a JWT (no token needed) |
| `GET`    | `/api/auth/me`            | Current user, with their default format (`BO1`/`BO3`) |
| `GET`    | `/api/users`              | Every registered player |
| `PUT`    | `/api/users/me/settings`  | Change the default format |
| `GET`    | `/api/guests`             | Opponents without an account |
| `POST`   | `/api/guests`             | Add a guest (returns the existing one if the name is known) |
| `GET`    | `/api/cards`              | Cards: `?type=legend`, `?type=battlefield`, `?q=name`, `?includeVariants=true` |
| `POST`   | `/api/cards/import`       | Refresh the cards from Riftcodex |
| `GET`    | `/api/decks`              | All decks, or `?ownerId=` |
| `GET`    | `/api/decks/{id}`         | One deck with its card list |
| `PUT`    | `/api/decks/{id}`         | Create or replace a deck (owner only, id chosen by the client) |
| `DELETE` | `/api/decks/{id}`         | Delete a deck (its matches keep their legend) |
| `GET`    | `/api/matches`            | Match summaries, or `?userId=` |
| `GET`    | `/api/matches/{id}`       | One match with its games and every point in order |
| `PUT`    | `/api/matches/{id}`       | Upload a finished match (201 the first time, 200 when re-sent) |
| `DELETE` | `/api/matches/{id}`       | Delete a match (its recorder or players only) |

Every endpoint except register and login needs `Authorization: Bearer <token>`.

The whole group sees each other's matches, decks and players. Only the owner of a deck,
and the recorder or players of a match, can change or delete it.

Match and deck ids are UUIDs generated by the client, so a phone can create them offline
and upload them later: sending the same match twice replaces it instead of duplicating it.
Scores are never typed in for live matches: they are derived from the list of points.
See `SPEC-v2.md` for the data model.

---

# Features

### Backend

- [x] JWT authentication
- [x] User registration
- [x] User login
- [x] User management
- [x] Match management
- [x] Round management
- [x] Deck management
- [x] Legend management
- [x] Match history
- [x] Statistics
- [x] Match filters
- [x] Request validation
- [x] Global exception handling
- [x] Flyway migrations
- [x] Service-layer tests

### Frontend

- [x] Dashboard
- [x] Navigation
- [x] Match history
- [ ] Play screen
- [ ] Deck management
- [ ] Detailed statistics
- [ ] Login screen
- [ ] Registration screen

---

# Deployment

The backend is designed to be deployed with Spring Boot on Render and use Neon as its PostgreSQL database.

Environment variables are configured directly in Render.

See `DEPLOY.md` for the complete deployment procedure.

---

# Documentation

| File              | Description                            |
| ----------------- | -------------------------------------- |
| `README.md`       | Installation and usage                 |
| `ARCHITECTURE.md` | Backend architecture                   |
| `FRONTEND.md`     | Frontend architecture and requirements |
| `DEPLOY.md`       | Deployment instructions                |

---

<br><br>

### Project

Riftlog started as a university project and is intended to continue as a personal application.

The goal is to provide a simple tool for Riftbound players to track their matches, decks, rounds, and performance.


Je reprend en Français ici, le projet devient désormais un projet personnel, et non plus un projet universitaire.

J'aimerais que Riftlog soit le compagnion ultime pour mes amis et moi lorsque nous jouons à Riftbound, des outils existent déjà mais sont payants et surtout incomplets.

L'idée serait donc d'avoir une appli pc ET mobile, les deux seront différentes mais synchronisées.

Ce projet a déjà avancé et les fondations mérititent d'être rétudiés, devraient-ont les garder, créer une branche à part, un nouveau répo ? 

Pour l'appli mobile il faut un compangnion capable de conmpter les points quand on joue IRL, tu as déjà les infos de ce que je recherche normalment, l'idée serait qu'à la fin de partie toutes les informations soit automatiquement stockées, points de conquer, hold, ability, dans quel ordre ils ont été marqués. Quelles légendes, quels battlefield, le système de bo3 est à intégrer. 

Si on fait un tournoi en ligne il faurdrait une option pou remplir manuellement les informations et ainsi garder un suivi IRL +online, le mieux à long terme serait de pouvoir récupérer direct les scores et analyse de riftatlas comme le fait Riftlite;

