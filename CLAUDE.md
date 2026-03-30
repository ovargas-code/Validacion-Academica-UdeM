# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**validacion-academica-ms** — Academic validation microservice for Universidad de Medellín. Issues PDF certificates with QR codes, verifies student identity via OTP email, and exposes a REST API consumed by a React frontend.

## Commands

### Backend (Spring Boot + Kotlin)

```bash
# Run with H2 in-memory (default dev mode, no Docker needed)
./gradlew bootRun --args="--server.port=8081"

# Run with PostgreSQL profile
./gradlew bootRun --args="--spring.profiles.active=postgres --server.port=8081"

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "co.edu.udemedellin.validacionacademica.usecase.CreateStudentUseCaseTest"

# Build JAR
./gradlew build
```

### Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev      # Dev server on http://localhost:5173
npm run build    # Production build
npm run lint     # ESLint
```

### Docker (full stack)

```bash
docker compose up --build            # All services
docker compose up postgres -d        # Only DB
```

## Architecture

Hexagonal architecture (ports & adapters):

```
domain/
  model/          — Pure domain entities (Student, ValidationRequest, etc.)
  ports/          — Interfaces: StudentRepositoryPort, MailPort, PdfGeneratorPort, etc.

application/
  usecase/        — One class per use case (CreateStudentUseCase, InitiateValidationUseCase, etc.)

infrastructure/
  rest/
    controller/   — HTTP endpoints (StudentController, ValidationController, AuthController, VerificationController)
    dto/          — Request/response DTOs
    exception/    — GlobalExceptionHandler
  persistence/
    entity/       — JPA entities
    repository/   — Spring Data JPA repos
    adapter/      — Port implementations (StudentPersistenceAdapter, etc.)
  email/          — MailServiceAdapter (real) + NoOpMailAdapter (dev)
  documents/      — PdfDocumentGeneratorAdapter (OpenPDF + ZXing QR)
  security/       — JwtTokenService, RateLimitFilter (Bucket4j + Caffeine)
  config/         — SecurityConfig, JwtProperties, OpenApiConfig
```

The frontend (`frontend/`) is a separate React app. Vite proxies `/api/*` to `http://localhost:8080` in dev mode.

## Key Flows

**Validation flow:** `POST /api/validations/initiate` (sends OTP) → `POST /api/validations/confirm` (verifies OTP, generates PDF, sends via email)

**Admin login:** `POST /api/auth/login` → returns JWT → used in `Authorization: Bearer <token>` header for protected endpoints (e.g., student history)

**Certificate verification:** `GET /api/verify/{token}` — public endpoint, no auth required

## Environment

Requires a `.env` file at project root (copy from `.env.example`). Key variables:

| Variable | Purpose |
|---|---|
| `ADMIN_USER` / `ADMIN_PASSWORD` | Spring Security in-memory admin |
| `JWT_SECRET` | HMAC-SHA256 signing key (min 32 chars) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail SMTP with app password |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Only used with `postgres` profile |

Default profile uses H2 in-memory (`jdbc:h2:mem:validaciondb`). H2 console is disabled by default; enable with `H2_CONSOLE_ENABLED=true`.

## Dev URLs (local)

| URL | Description |
|---|---|
| http://localhost:5173 | React frontend (Vite dev) |
| http://localhost:8081 | Backend + embedded web portal |
| http://localhost:8081/swagger-ui/index.html | Swagger UI |
| http://localhost:8081/h2-console | H2 console (when enabled) |
