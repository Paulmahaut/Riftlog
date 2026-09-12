# HOW TO LAUNCH THE PROJECT

1. make sure to have well install `maven java` and the `npm package of vue.js`, and that `docker` / `docker compose` is available.

<br>

2. from the repo root, start the Postgres database :
```cmd
docker compose up -d
```
*this starts Postgres on* ```localhost:5432``` *(db/user/password: `riftlog`, see `docker-compose.yml`)*

3. go to `/backend` and then launch the backend (the server) :
```cmd
chmod +x mvnw
./mvnw spring-boot:run
```
*this opens a gate on* ```http://localhost:8080```

Flyway creates the schema automatically on startup, but there is no seeded account:
you must register a real user before the API will do anything else, since every
match/deck is owned by whoever is authenticated. Either through the frontend's
register screen once it exists, or directly:
```cmd
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"you@example.com\",\"password\":\"changeme123\",\"displayName\":\"You\"}"
```
This returns a JWT `token` — send it as `Authorization: Bearer <token>` on every
other `/api/**` call (everything except `/api/auth/register` and `/api/auth/login`
requires it; the API is stateless, there is no server-side session/cookie).

4. go to `/frontend`and launch the interface that allow you to see the app :
```cmd
npm run dev
```
*this opens a gate on* ```http://localhost:5173```

5. Go to : (you'll see the app)
```cmd
http://localhost:5173
```