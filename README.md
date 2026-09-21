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

| Method | Endpoint             | Authentication |
| ------ | -------------------- | -------------- |
| `POST` | `/api/auth/register` | No             |
| `POST` | `/api/auth/login`    | No             |
| `GET`  | `/api/auth/me`       | Yes            |
| `GET`  | `/api/matches`       | Yes            |
| `GET`  | `/api/matches/{id}`  | Yes            |
| `POST` | `/api/matches`       | Yes            |
| `GET`  | `/api/stats`         | Yes            |

User data is isolated: matches and decks are associated with the authenticated account.

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
