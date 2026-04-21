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
  model/          — Pure domain entities (Student, ValidationRequest, SolicitudEmpresa, etc.)
  ports/          — Interfaces: StudentRepositoryPort, MailPort, PdfGeneratorPort,
                    SolicitudEmpresaRepositoryPort, FileStoragePort, etc.

application/
  usecase/        — One class per use case (CreateStudentUseCase, InitiateValidationUseCase,
                    CrearSolicitudEmpresaUseCase, ConsultarSolicitudEmpresaUseCase,
                    GenerarNumeroSolicitudService, etc.)

infrastructure/
  rest/
    controller/   — HTTP endpoints (StudentController, ValidationController, AuthController,
                    VerificationController, AuditController, SolicitudEmpresaController)
    dto/          — Request/response DTOs
    exception/    — GlobalExceptionHandler
  persistence/
    entity/       — JPA entities
    repository/   — Spring Data JPA repos
    adapter/      — Port implementations (StudentPersistenceAdapter,
                    SolicitudEmpresaPersistenceAdapter, etc.)
  storage/        — LocalFileSystemStorageAdapter (FileStoragePort implementation)
  email/          — MailServiceAdapter (real) + NoOpMailAdapter (dev)
  documents/      — PdfDocumentGeneratorAdapter (OpenPDF + ZXing QR)
  security/       — JwtTokenService, RateLimitFilter (Bucket4j + Caffeine)
  config/         — SecurityConfig, JwtProperties, OpenApiConfig, StorageConfig, StorageProperties
```

The frontend (`frontend/`) is a separate React app. Vite proxies `/api/*` to `http://localhost:8080` in dev mode.

## Key Flows

**Validation flow (individual/OTP):** `POST /api/validations/initiate` (sends OTP) → `POST /api/validations/confirm` (verifies OTP, generates PDF, sends via email)

**Enterprise validation flow:** `POST /api/v1/solicitudes-empresa` (multipart: JSON part `datos` + PDF part `carta`) → returns `numeroSolicitud` in format `SOL-YYYYMMDD-NNNNNN`. Separate flow from the OTP flow — no email verification step.

**Admin login:** `POST /api/auth/login` → returns JWT → used in `Authorization: Bearer <token>` header for protected endpoints (e.g., student history)

**Certificate verification:** `GET /api/v1/verificaciones/{code}` — public endpoint, no auth required

## Environment

Requires a `.env` file at project root (copy from `.env.example`). Key variables:

| Variable | Purpose |
|---|---|
| `ADMIN_USER` / `ADMIN_PASSWORD` | Spring Security in-memory admin |
| `JWT_SECRET` | HMAC-SHA256 signing key (min 32 chars) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Gmail SMTP with app password |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Only used with `postgres` profile |
| `UPLOAD_DIR` | Base directory for uploaded files (default: `./uploads`) |

Default profile uses H2 in-memory (`jdbc:h2:mem:validaciondb`). H2 console is disabled by default; enable with `H2_CONSOLE_ENABLED=true`.

## Dev URLs (local)

| URL | Description |
|---|---|
| http://localhost:3000 | React frontend (Vite dev — `npm run dev`, `strictPort: true`) |
| http://localhost:8080 | Backend + embedded web portal |
| http://localhost:8080/swagger-ui/index.html | Swagger UI |
| http://localhost:8080/h2-console | H2 console (when enabled) |

## Enterprise Solicitud Endpoints (public, rate-limited)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/solicitudes-empresa` | Create enterprise validation request. Multipart: `dados` (JSON) + `carta` (PDF ≤ 10 MB). Returns `numeroSolicitud` as `SOL-YYYYMMDD-NNNNNN`. |
| `GET` | `/api/v1/solicitudes-empresa/{numeroSolicitud}` | Query by radicado number. |
| `GET` | `/api/v1/solicitudes-empresa/recursos/plantilla-carta` | Download the `.docx` template. |

## File Storage

Uploaded PDFs are stored at `{UPLOAD_DIR}/solicitudes-empresa/{yyyy}/{MM}/{uuid}.pdf`.
The template `.docx` lives at `src/main/resources/static/plantillas/carta-autorizacion-verificacion-academica.docx`.

## Tech Debt — Pending Flyway Migration

The schema is managed by Hibernate `ddl-auto`. Migrating to Flyway is a **pending task** for the next hardening cycle. All tables (including `solicitudes_empresa`) are currently created by Hibernate on startup.
