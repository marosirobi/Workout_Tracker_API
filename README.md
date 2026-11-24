# Workout Tracker API

REST backend for the Workout Tracker mobile app: user accounts, strength and cardio workouts, GPS tracks, workout templates, exercise statistics and personal records.

## Tech stack

- Java 26, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Actuator)
- JWT authentication (access token + rotating refresh tokens), Argon2 password hashing
- H2 (development) and PostgreSQL 18 (production)
- Lombok, springdoc-openapi (Swagger UI in development)
- JUnit, AssertJ, Mockito, MockMvcTester
- Docker and Docker Compose for production

## Running the backend

There are two ways to run it. Neither requires a locally installed PostgreSQL.

### Option A: development mode (H2, no Docker)

Requirements: JDK 26. Maven is not needed, the Maven Wrapper downloads it.

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

The `dev` profile is active by default (`spring.profiles.active=dev`), so:

- the server listens on `http://localhost:8082`
- data is stored in an H2 file database under `data/` (created automatically)
- a development JWT key is used from `application-dev.properties`, no `.env` needed
- the exercise library is seeded and a test user is created (`bajnok@gmail.com` / `jelszo`)
- Swagger UI: `http://localhost:8082/swagger-ui/index.html`
- H2 console: `http://localhost:8082/h2-console` (JDBC URL `jdbc:h2:file:./data/fitnessdb`, user `sa`, empty password)

You can also start `WorkouttrackerapiApplication` directly from IntelliJ IDEA.

### Option B: production mode (Docker, PostgreSQL)

Requirements: Docker (Docker Desktop on Windows). Java is not needed, the image is built inside Docker.

1. Create the `.env` file from the template:

   ```bash
   cp .env.example .env
   ```

2. Fill in `DB_PASSWORD` and `JWT_SECRET`. Both should be long random values; `JWT_SECRET` must be Base64 and at least 256 bits. To generate one:

   ```powershell
   # PowerShell
   $b = New-Object byte[] 64; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
   ```

   ```bash
   # Linux / macOS
   openssl rand -base64 64 | tr -d '\n'
   ```

   Keep the values in the `.env` file itself (not only in the shell session), otherwise a later `docker compose up` recreates the containers with empty values. `.env` is git-ignored.

3. Build and start:

   ```bash
   docker compose up -d --build
   ```

4. Check that it is running:

   ```bash
   docker compose ps
   docker compose logs backend --tail 20
   curl http://localhost:8081/actuator/health     # {"status":"UP"}
   ```

What Compose starts:

| Service | Description |
|---|---|
| `db` | PostgreSQL 18, data kept in the `pgdata` volume, not reachable from outside |
| `backend` | the API with the `prod` profile, published only on `127.0.0.1:8081` |

The containers restart automatically (`restart: unless-stopped`) and log size is limited to 3 x 10 MB per container.

The backend is bound to `127.0.0.1`, so it is not reachable from the local network. In our setup it is exposed to the internet with Tailscale Funnel (`tailscale funnel --bg 8081`), which provides a public HTTPS address and forwards the real client IP in `X-Forwarded-For`.

Port note: the React Native Metro bundler also uses port 8081 by default. If you develop the mobile app on the same machine, either stop the backend container while Metro runs, or change the host port in `compose.yaml` (for example `"127.0.0.1:8090:8081"`) and point Tailscale Funnel to the new port.

Useful commands:

```bash
docker compose stop backend      # stop only the API
docker compose start backend
docker compose up -d --build     # rebuild after code changes
docker compose down              # stop everything (data stays in the volume)
```

## Profiles

| Profile | Used by | Database | Port | Notes |
|---|---|---|---|---|
| `dev` | local development (default) | H2 file | 8082 | Swagger, H2 console, SQL logging, test data |
| `prod` | Docker | PostgreSQL | 8081 | values from environment variables, only `/actuator/health` exposed |
| `test` | automated tests | H2 in memory | none (mock MVC) | relaxed registration limit |

## Configuration

Production values come from environment variables (see `.env.example` and `compose.yaml`):

| Variable | Description |
|---|---|
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL database, user and password |
| `JWT_SECRET` | Base64 HMAC key for signing access tokens |

Other settings in `application.properties`:

| Property | Default | Description |
|---|---|---|
| `app.jwt.access-expiration-ms` | 900000 (15 min) | access token lifetime |
| `app.jwt.refresh-expiration-ms` | 604800000 (7 days) | refresh token lifetime |
| `app.rate-limit.login-per-15-min` | 10 | login attempts per email address |
| `app.rate-limit.register-per-hour` | 10 | registrations per IP address |

## Security

- Passwords: 8 to 128 characters with at least one letter, one digit and one special character; hashed with Argon2.
- Access tokens are short-lived JWTs; refresh tokens are stored per device, rotated on every use and revoked on logout and password change.
- Login and registration are rate limited; exceeding the limit returns `429` with code `TOO_MANY_REQUESTS`.
- Errors are returned in a common `ApiError` format, with a machine-readable `code` where the client needs one (`EMAIL_TAKEN`, `USERNAME_TAKEN`, `WRONG_CURRENT_PASSWORD`, `SAME_PASSWORD`, `TOO_MANY_REQUESTS`).
- Every request is logged on one line with method, path, status, duration, user id and client IP.

## API overview

All endpoints are under `/api`. Everything except `/api/auth/**` requires an `Authorization: Bearer <token>` header. Full documentation is available in Swagger UI in development mode.

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| Current user | `GET /users/me`, `PUT /users/me`, `PUT /users/me/password`, `DELETE /users/me` |
| Workouts | `GET /workouts`, `GET /workouts/range`, `GET /workouts/{id}`, `POST /workouts`, `POST /workouts/{id}/cardio-data`, `PUT /workouts/{id}`, `PUT /workouts/{id}/edit`, `DELETE /workouts/{id}` |
| Templates | `GET /templates`, `POST /templates`, `PUT /templates/{id}`, `DELETE /templates/{id}` |
| Exercises | `GET /exercises`, `GET /exercises/{id}`, `GET /exercises/{id}/stats` |
| Health | `GET /actuator/health` (outside `/api`, public) |

## Tests

```bash
./mvnw test
```

- unit tests for services, mapper, utilities and request validation
- repository tests with `@DataJpaTest`
- integration tests (`src/test/java/.../integration`) that start the full application against an in-memory H2 database and test the auth and workout flows end to end

## Backups (production)

`scripts/backup.ps1` creates a compressed `pg_dump` of the Docker database into `H:\backups\workouttracker` and deletes backups older than 14 days. Adjust `$backupDir` for your machine.

Run it manually:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\backup.ps1
```

Schedule it daily with Windows Task Scheduler:

```powershell
$action = New-ScheduledTaskAction -Execute "powershell.exe" `
    -Argument '-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "<path to project>\scripts\backup.ps1"'
$trigger = New-ScheduledTaskTrigger -Daily -At 3:00
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Minutes 10)
Register-ScheduledTask -TaskName "WorkoutTracker backup" -Action $action -Trigger $trigger -Settings $settings
```

Restore a backup (this overwrites the current data):

```powershell
docker compose stop backend
docker compose cp <backup file>.dump db:/tmp/restore.dump
docker compose exec -T db sh -c 'pg_restore --clean --if-exists -U "$POSTGRES_USER" -d "$POSTGRES_DB" /tmp/restore.dump'
docker compose start backend
```

## Project structure

```
src/main/java/com/marosirobi/workouttrackerapi
  config/       security, JWT filter, request logging, CORS, data seeding
  controller/   REST controllers
  domain/       JPA entities and enums
  dto/          request and response records
  exception/    custom exceptions and the global exception handler
  mapper/       entity to DTO mapping
  repository/   Spring Data JPA repositories
  service/      business logic
  utils/        helpers (rate limiter, password policy, geo and volume calculations, log masking)
src/main/resources
  application.properties        shared settings
  application-dev.properties    development profile
  application-prod.properties   production profile
scripts/backup.ps1              database backup
Dockerfile, compose.yaml        production containers
```
