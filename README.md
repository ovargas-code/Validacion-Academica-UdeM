# Validación Académica — Universidad de Medellín

Sistema de validación académica con generación de certificados PDF, verificación de identidad por OTP y envío automático por correo electrónico.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)

---

## Descripción

Microservicio REST construido con **arquitectura hexagonal** (puertos y adaptadores) que permite:

- Consultar el estado académico de un estudiante por número de documento
- Verificar la identidad del solicitante mediante un código OTP enviado a su correo
- Generar un certificado PDF firmado con código QR de validación
- Enviar el certificado por correo electrónico (Gmail SMTP)
- Verificar certificados emitidos mediante URL pública

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.3.2 + Kotlin 1.9 |
| Seguridad | Spring Security 6 + JWT (HMAC-SHA256) |
| Base de datos | PostgreSQL 16 (producción) / H2 (desarrollo) |
| PDF | OpenPDF + ZXing (QR codes) |
| Correo | Jakarta Mail — Gmail SMTP SSL/465 |
| Frontend | React 19 + Vite + Nginx |
| Infraestructura | Docker Compose |
| Documentación | SpringDoc OpenAPI (Swagger UI) |

---

## Flujo de validación

```
Solicitante                  Backend                      Gmail
     │                          │                            │
     │── POST /api/validations/initiate ──────────────────► │
     │        { documento, email, tipo }                     │
     │                          │                            │
     │                     Consulta BD                       │
     │                          │                            │
     │                    Estado VALID?                      │
     │                          │── Envía código OTP ──────► │
     │◄── { token, maskedEmail }│                            │
     │                          │                            │
     │── POST /api/validations/confirm ──────────────────    │
     │        { token, otp }                                 │
     │                          │                            │
     │                   Verifica OTP                        │
     │                          │                            │
     │                   Genera PDF + QR                     │
     │                          │── Envía certificado ─────► │
     │◄── PDF (descarga)        │                            │
```

---

## Requisitos previos

- **Java 21** (JDK)
- **Docker Desktop** con WSL2 habilitado (Windows)
- Cuenta Gmail con **contraseña de aplicación** de 16 caracteres ([cómo generarla](https://myaccount.google.com/apppasswords))

---

## Configuración inicial

### 1. Crear archivo `.env`

```powershell
Copy-Item .env.example .env
```

### 2. Editar `.env` con valores reales

```env
# Administrador del sistema
ADMIN_USER=admin
ADMIN_PASSWORD=udem2026

# JWT — genera uno seguro con: openssl rand -base64 48
JWT_SECRET=cambia-esto-por-un-secreto-aleatorio-de-al-menos-32-caracteres
JWT_EXPIRATION_MS=3600000

# Puerto local (H2/dev)
SERVER_PORT=8081

# Base de datos PostgreSQL
DB_URL=jdbc:postgresql://localhost:5432/validacion_academica
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Correo Gmail (usa contraseña de aplicación, no tu contraseña personal)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=465
MAIL_USERNAME=tu_correo@gmail.com
MAIL_PASSWORD=xxxx_xxxx_xxxx_xxxx
MAIL_SSL_ENABLE=true
MAIL_STARTTLS_ENABLE=false
```

> **Nota:** El archivo `.env` está en `.gitignore` — nunca se sube al repositorio.

---

## Modos de ejecución

### Modo 1 — Desarrollo local (H2 en memoria)

Ideal para desarrollo rápido sin Docker.

```powershell
# Backend
.\gradlew.bat bootRun --args="--server.port=8081"

# Frontend (en otra terminal)
cd frontend
npm install
npm run dev
```

**Accesos:**
| URL | Descripción |
|-----|-------------|
| http://localhost:5173 | Frontend React |
| http://localhost:8081 | Portal web |
| http://localhost:8081/swagger-ui/index.html | API docs |
| http://localhost:8081/h2-console | Consola H2 (JDBC: `jdbc:h2:mem:validaciondb`) |

---

### Modo 2 — PostgreSQL en Docker + app local

```powershell
# Levantar solo la base de datos
docker compose up postgres -d

# Ejecutar app con perfil postgres
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres --server.port=8081"

# Frontend
cd frontend && npm run dev
```

---

### Modo 3 — Todo en Docker (producción local)

```powershell
docker compose up --build
```

Para aplicar cambios de código:

```powershell
docker compose down
docker compose build --no-cache
docker compose up
```

**Accesos:**
| URL | Descripción |
|-----|-------------|
| http://localhost:3000 | Frontend React |
| http://localhost:8080 | Portal web |
| http://localhost:8080/swagger-ui/index.html | API docs |

> En Docker el puerto del backend es **8080** (no 8081).

---

## API — Endpoints principales

### Autenticación

```http
POST /api/auth/login
Content-Type: application/json

{ "username": "admin", "password": "udem2026" }
```

Devuelve un JWT. Usarlo en Swagger con **Authorize → Bearer `<token>`**.

### Validación académica

```http
POST /api/validations/initiate      # Inicia validación, envía OTP
POST /api/validations/confirm       # Verifica OTP, devuelve PDF
GET  /api/v1/verificaciones/{uuid}  # Verifica autenticidad de un certificado
```

### Gestión de estudiantes (requiere JWT con rol ADMIN)

```http
POST /api/v1/students               # Registrar estudiante
GET  /api/v1/students               # Listar todos
GET  /api/v1/students/{documento}   # Buscar por documento
```

---

## Estructura del proyecto

```
src/main/kotlin/.../
├── application/
│   └── usecase/              # Casos de uso (lógica de negocio)
├── domain/
│   ├── model/                # Entidades del dominio
│   └── ports/                # Interfaces de puertos
└── infrastructure/
    ├── config/               # SecurityConfig, JwtProperties, OpenApiConfig
    ├── email/                # MailServiceAdapter (Gmail SMTP)
    ├── persistence/          # Entidades JPA, repositorios
    ├── rest/
    │   ├── controller/       # Controllers REST y web
    │   └── dto/              # Request/Response DTOs
    └── security/             # JwtTokenService, RateLimitFilter
```

---

## Diagnóstico

**Ver logs del contenedor:**
```powershell
docker logs validacion-app --follow
```

**Ver logs de PostgreSQL:**
```powershell
docker logs validacion-postgres
```

**Puerto ocupado:**
```powershell
netstat -ano | findstr :8080
```

**Contenedores activos:**
```powershell
docker ps -a
```

**Logs esperados al enviar correo correctamente:**
```
INFO MailServiceAdapter: Preparando envío SMTP host=smtp.gmail.com puerto=465 ...
INFO MailServiceAdapter: Código OTP enviado a usuario@ejemplo.com
INFO MailServiceAdapter: Certificado enviado correctamente a usuario@ejemplo.com
```

---

## Seguridad

- Autenticación JWT con HMAC-SHA256 y expiración configurable
- Rate limiting por IP: 5 req/min en login, 10 req/min en validaciones
- CORS configurable vía `CORS_ALLOWED_ORIGINS`
- Headers de seguridad: CSP, HSTS, Referrer-Policy, Permissions-Policy
- OTP de 6 dígitos con expiración de 10 minutos y uso único
- H2 Console excluida completamente de Spring Security (solo perfil dev)

---

## Variables de entorno — referencia completa

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `ADMIN_USER` | Usuario administrador | `admin` |
| `ADMIN_PASSWORD` | Contraseña administrador | `udem2026` |
| `JWT_SECRET` | Clave HMAC para tokens JWT | `changeme-...` |
| `JWT_EXPIRATION_MS` | Expiración del JWT en ms | `3600000` (1h) |
| `SERVER_PORT` | Puerto del backend local | `8080` |
| `DB_URL` | URL JDBC de PostgreSQL | — |
| `DB_USERNAME` | Usuario de BD | `postgres` |
| `DB_PASSWORD` | Contraseña de BD | `postgres` |
| `MAIL_HOST` | Servidor SMTP | `smtp.gmail.com` |
| `MAIL_PORT` | Puerto SMTP | `465` |
| `MAIL_USERNAME` | Correo remitente | — |
| `MAIL_PASSWORD` | Contraseña de aplicación Gmail | — |
| `MAIL_SSL_ENABLE` | SSL directo (puerto 465) | `true` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos CORS | `http://localhost:3000` |
