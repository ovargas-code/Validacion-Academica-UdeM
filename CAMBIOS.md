# Registro de Cambios — validacion-academica-ms

---

## [feature/formulario-solicitud-completo] — 21 de abril de 2026

### Nuevo flujo: Solicitud de Verificación Académica por Empresas

Implementación completa del formulario de solicitud para empresas o entidades externas que
requieren verificar el estado académico de un estudiante o egresado de la Universidad de Medellín.
Flujo independiente del flujo OTP existente (`initiate → confirm`). No se modificó ninguna
pieza del flujo anterior.

#### Archivos nuevos — Backend (12 en dominio/aplicación, 8 en infraestructura)

| Capa | Archivo | Descripción |
|---|---|---|
| Dominio | `domain/model/SolicitudEmpresa.kt` | Modelo de dominio puro |
| Dominio | `domain/model/EstadoSolicitud.kt` | Enum: PENDIENTE / EN_REVISION / APROBADA / RECHAZADA |
| Dominio | `domain/model/TipoDocumento.kt` | Enum: CC / CE / PAS / TI |
| Dominio | `domain/ports/SolicitudEmpresaRepositoryPort.kt` | Puerto de persistencia |
| Dominio | `domain/ports/FileStoragePort.kt` | Puerto de almacenamiento de archivos |
| Aplicación | `usecase/CrearSolicitudEmpresaUseCase.kt` | Orquesta almacenamiento + número + persistencia |
| Aplicación | `usecase/ConsultarSolicitudEmpresaUseCase.kt` | Consulta por número de radicado |
| Aplicación | `usecase/GenerarNumeroSolicitudService.kt` | Genera SOL-YYYYMMDD-NNNNNN con consecutivo diario |
| Infraestructura | `persistence/entity/SolicitudEmpresaEntity.kt` | Tabla `solicitudes_empresa` |
| Infraestructura | `persistence/repository/SolicitudEmpresaJpaRepository.kt` | Spring Data JPA + query por fecha |
| Infraestructura | `persistence/adapter/SolicitudEmpresaPersistenceAdapter.kt` | Implementa el puerto |
| Infraestructura | `storage/LocalFileSystemStorageAdapter.kt` | Escribe en disco con path-traversal guard |
| Infraestructura | `config/StorageProperties.kt` | `@ConfigurationProperties("app.storage")` |
| Infraestructura | `config/StorageConfig.kt` | Registra StorageProperties en el contexto |
| Infraestructura | `rest/dto/CrearSolicitudEmpresaRequest.kt` | DTO multipart con Bean Validation |
| Infraestructura | `rest/dto/SolicitudEmpresaResponse.kt` | DTO de respuesta (omite rutaCarta) |
| Infraestructura | `rest/controller/SolicitudEmpresaController.kt` | 3 endpoints públicos |

#### Archivos modificados — Backend

| Archivo | Cambio |
|---|---|
| `application.yml` | `app.storage.upload-dir`, multipart 10MB/15MB, rate-limit rule para solicitudes-empresa |
| `application-test.yml` | `app.rate-limit.rules: []` — desactiva rate limiting en tests para evitar HTTP 429 |
| `GlobalExceptionHandler.kt` | Handler `MaxUploadSizeExceededException` → HTTP 413 |
| `SecurityConfig.kt` | `/api/v1/solicitudes-empresa/**` agregado como ruta pública |

#### Endpoints nuevos

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/solicitudes-empresa` | Público | Multipart: `datos` (JSON) + `carta` (PDF ≤ 10 MB). Retorna `SOL-YYYYMMDD-NNNNNN`. |
| `GET` | `/api/v1/solicitudes-empresa/{numeroSolicitud}` | Público | Consulta por número de radicado |
| `GET` | `/api/v1/solicitudes-empresa/recursos/plantilla-carta` | Público | Descarga plantilla `.docx` |

#### Archivos nuevos — Frontend

| Archivo | Descripción |
|---|---|
| `pages/SolicitudEmpresa.jsx` | Formulario en 3 secciones, validación cliente, upload PDF, checkbox consentimiento, panel de éxito |
| `pages/SolicitudEmpresa.css` | Estilos propios (hereda design system de `index.css`) |

#### Archivos modificados — Frontend

| Archivo | Cambio |
|---|---|
| `vite.config.js` | `server.port: 3000, strictPort: true` fusionado con proxy existente |
| `App.jsx` | Ruta `/solicitar-empresa` |
| `Navbar.jsx` | Link "Empresas" entre "Solicitar" y "Verificar" |
| `api.js` | `crearSolicitudEmpresa`, `consultarSolicitud`, `urlPlantillaCarta` |

#### Tests nuevos — 12 tests (todos en verde, total suite: 67 tests)

| Archivo | Tests |
|---|---|
| `usecase/CrearSolicitudEmpresaUseCaseTest.kt` | Creación exitosa, `aceptaTerminos=false` rechazado, fallo de storage propagado |
| `usecase/ConsultarSolicitudEmpresaUseCaseTest.kt` | Número existente retorna solicitud, número inexistente retorna null |
| `controller/SolicitudEmpresaControllerIntegrationTest.kt` | POST válido 201, consecutivos únicos, aceptaTerminos false 400, NIT inválido 400, correo inválido 400, carta no-PDF 400, GET existente 200, GET inexistente 404, número inválido 400, plantilla 200 |

#### Deuda técnica documentada

> **Flyway pendiente.** El esquema de la nueva tabla `solicitudes_empresa` (y todas las demás)
> es gestionado por Hibernate con `ddl-auto`. La migración a Flyway está pendiente para el
> próximo ciclo de hardening. En producción con PostgreSQL, cambios de esquema requieren
> recrear la BD o aplicarlos manualmente hasta que Flyway esté configurado.

> **Atomicidad disco/BD.** Si la transacción de BD falla después de que el PDF fue escrito
> en disco, el archivo quedará huérfano en `uploads/`. Pendiente: implementar limpieza
> periódica o migrar a almacenamiento transaccional (MinIO + outbox pattern).

---

**Fecha:** 24 de marzo de 2026
**Rama:** `main`
**Commits incluidos:** `bd54362` → `b183ab3`

---

## Resumen ejecutivo

Se realizó una revisión completa del proyecto con corrección de bugs críticos, mejoras de arquitectura, robustecimiento del manejo de excepciones y despliegue del stack completo (backend + frontend) con Docker Compose.

---

## 1. Bugs corregidos

### 1.1 Nombre incorrecto en el certificado PDF al re-descargar
**Archivo:** `application/usecase/GenerateCertificatePdfUseCase.kt`
**Problema:** El campo `studentName` se poblaba con `validation.requesterName` (el nombre de la empresa o persona que solicitó la validación), no con el nombre real del estudiante. Cualquier descarga de certificado por código de verificación mostraba el nombre del solicitante en lugar del graduado.
**Corrección:**
```kotlin
// Antes
studentName = validation.requesterName

// Después
studentName = student?.fullName ?: "Nombre no disponible"
```

---

### 1.2 Generación duplicada del PDF
**Archivos:** `application/usecase/CreateValidationUseCase.kt` · `infrastructure/rest/controller/ValidationController.kt`
**Problema:** El PDF del certificado se generaba **dos veces**: una dentro de `CreateValidationUseCase` (para adjuntarlo al correo) y otra en `ValidationController` (para devolverlo en la respuesta HTTP), con datos diferentes entre sí.
**Corrección:**
- Se añadió `pdfBytes: ByteArray?` a `ValidationExecutionResponse`.
- El caso de uso genera el PDF **una sola vez** y lo almacena en la respuesta.
- El controller reutiliza los bytes ya generados.
- Se eliminó la inyección innecesaria de `PdfGeneratorPort` en el controller.

---

### 1.3 El endpoint de validación devolvía PDF aunque el estudiante no existiera
**Archivo:** `infrastructure/rest/controller/ValidationController.kt`
**Problema:** `POST /api/validations/verify` siempre generaba y devolvía un PDF, independientemente de si el resultado era `VALID`, `REQUIRES_REVIEW` o `NOT_FOUND`. En casos negativos devolvía un certificado sin sentido.
**Corrección:**
- Para `VALID` → HTTP 200 con el certificado PDF.
- Para `REQUIRES_REVIEW` o `NOT_FOUND` → HTTP 422 con cuerpo JSON (`ValidationResponseDto`) que describe el resultado.
- Se amplió `produces` del endpoint para aceptar tanto `application/pdf` como `application/json`.

```kotlin
// Comportamiento nuevo
if (response.result.status != ValidationStatus.VALID || response.pdfBytes == null) {
    return ResponseEntity.unprocessableEntity()
        .contentType(MediaType.APPLICATION_JSON)
        .body(ValidationResponseDto(...))
}
return ResponseEntity.ok().headers(headers).body(response.pdfBytes)
```

---

### 1.4 Test de contexto de Spring no encontraba la clase principal
**Archivo:** `test/.../ValidacionAcademicaApplicationTests.kt`
**Problema:** `@SpringBootTest` sin parámetro `classes` fallaba porque `@SpringBootApplication` está en el subpaquete `bootstrap`, no en el paquete raíz donde vive el test. Error: *"Unable to find a @SpringBootConfiguration"*.
**Corrección:**
```kotlin
// Antes
@SpringBootTest
class ValidacionAcademicaApplicationTests

// Después
@SpringBootTest(classes = [ValidacionAcademicaApplication::class])
class ValidacionAcademicaApplicationTests
```

---

### 1.5 Incompatibilidad entre Spring Boot 3.3.2 y Gradle 9 (fallo en Docker)
**Archivo:** `gradle/wrapper/gradle-wrapper.properties`
**Problema:** `./gradlew bootJar` fallaba dentro del contenedor Docker con:
> `'java.lang.Integer org.gradle.api.file.CopyProcessingSpec.getDirMode()'`
El método `getDirMode()` fue eliminado en Gradle 9.0, pero el plugin de Spring Boot 3.3.2 aún lo usa. Los tests locales pasaban porque `./gradlew test` no ejecuta `bootJar`.
**Corrección:** Downgrade de Gradle 9.4.1 → **8.13** (última versión 8.x, totalmente compatible con Spring Boot 3.3.x).

---

### 1.6 Fechas `LocalDate` no se deserializaban desde JSON
**Archivo:** `src/main/resources/application.yml`
**Problema:** Jackson, sin configuración explícita, intentaba deserializar `LocalDate` como array de enteros `[2024, 6, 15]` en lugar de cadena ISO `"2024-06-15"`. Cualquier petición al endpoint de estudiantes que incluyera `graduationDate` devolvía HTTP 400.
**Corrección:**
```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
```

---

## 2. Manejo de excepciones mejorado

### 2.1 Nuevo handler para errores de deserialización de Jackson
**Archivo:** `infrastructure/rest/exception/GlobalExceptionHandler.kt`
**Problema:** Cuando se enviaba un valor inválido para un enum (p. ej. `"validationType": "INVALIDO"`), Jackson lanzaba `HttpMessageNotReadableException`. Al no haber handler específico, caía en el manejador genérico de `Exception` y devolvía HTTP 500.
**Corrección:** Nuevo handler que retorna HTTP 400 con un mensaje descriptivo que incluye los valores válidos del enum:

```
HTTP 400
{
  "error": "Solicitud inválida",
  "message": "Valor 'INVALIDO' no es válido para el campo 'validationType'. Valores permitidos: DEGREE, ENROLLMENT"
}
```

---

## 3. Arquitectura hexagonal — violación de capas corregida

### 3.1 Capa de aplicación importaba DTO de infraestructura
**Archivos:** `application/usecase/VerifyCertificateUseCase.kt` · `infrastructure/rest/controller/VerificationController.kt`
**Problema:** `VerifyCertificateUseCase` importaba `CertificateVerificationResponse` del paquete `infrastructure.rest.dto`, haciendo que la capa de aplicación dependiera de la capa REST — inversión de la dirección de dependencias.
**Corrección:**
- Se creó `CertificateInfo` como clase de datos en la capa de aplicación.
- `VerifyCertificateUseCase` ahora retorna `CertificateInfo`.
- `VerificationController` mapea `CertificateInfo` → `CertificateVerificationResponse` en la capa de infraestructura (donde corresponde).

---

## 4. Resiliencia — correo opcional sin romper el contexto

### 4.1 Fallo de contexto cuando el correo no está configurado
**Archivos:** `infrastructure/email/MailServiceAdapter.kt` · `infrastructure/email/NoOpMailAdapter.kt` *(nuevo)*
**Problema:** `MailServiceAdapter` inyectaba `JavaMailSender` incondicionalmente. Si Spring Boot no creaba el bean de correo (host no configurado), el contexto de Spring fallaba al arrancar.
**Corrección:**
- `MailServiceAdapter` ahora solo se activa con `@ConditionalOnBean(JavaMailSender::class)`.
- Se creó `NoOpMailAdapter` con `@ConditionalOnMissingBean(JavaMailSender::class)`: implementa `MailPort` registrando un `WARN` y omitiendo el envío de forma silenciosa. Garantiza que siempre existe un `MailPort` en el contexto.

---

## 5. Calidad de código

### 5.1 Configuración CSRF redundante
**Archivo:** `infrastructure/config/SecurityConfig.kt`
La llamada `csrf.ignoringRequestMatchers("/h2-console/**")` precedía inmediatamente a `csrf.disable()`, haciéndola completamente inoperante.
**Corrección:** Simplificado a `.csrf { it.disable() }`.

### 5.2 Logging con `println` en adaptador PDF
**Archivo:** `infrastructure/documents/PdfDocumentGeneratorAdapter.kt`
Se reemplazaron todas las llamadas `println(...)` por un logger SLF4J (`LoggerFactory.getLogger(...)`), integrando los mensajes en el sistema de logging estándar de Spring Boot.

### 5.3 Health indicator de correo mostraba estado DOWN
**Archivo:** `src/main/resources/application.yml`
El health indicator de Spring Boot intentaba conectarse al servidor SMTP. Sin correo configurado, `/actuator/health` devolvía `{"status":"DOWN"}` aunque la aplicación funcionara correctamente.
**Corrección:**
```yaml
management:
  health:
    mail:
      enabled: false
```

---

## 6. Código muerto eliminado

| Archivo eliminado | Motivo |
|---|---|
| `domain/ports/EmailSenderPort.kt` | Interfaz nunca implementada ni usada en ninguna parte del proyecto. Duplicaba conceptualmente `MailPort`. |
| `infrastructure/documents/QrCodeGenerator.kt` | Objeto utilitario nunca referenciado. `PdfDocumentGeneratorAdapter` tenía su propia lógica inline de generación de QR. |

---

## 7. Despliegue Docker — frontend React

### 7.1 Dockerfile del frontend
**Archivo:** `frontend/Dockerfile` *(nuevo)*
Build multi-etapa:
- **Etapa 1 (builder):** Node 22 Alpine — instala dependencias con `npm ci` y compila con `vite build`.
- **Etapa 2 (runtime):** Nginx Alpine — sirve el directorio `dist/` generado.

### 7.2 Configuración de Nginx
**Archivo:** `frontend/nginx.conf` *(nuevo)*
- Sirve la SPA React con `try_files $uri $uri/ /index.html` (necesario para React Router).
- Hace proxy de todas las rutas `/api/` al servicio backend `app:8080` dentro de la red Docker.

### 7.3 Servicio frontend en Docker Compose
**Archivo:** `docker-compose.yml`
Se añadió el servicio `frontend` que construye desde `./frontend` y expone el puerto 3000.

**Arquitectura de red resultante:**
```
Browser → http://localhost:3000 (Nginx)
               ├── /* ──────────────→ React SPA (archivos estáticos)
               └── /api/* ──────────→ validacion-app:8080 (Spring Boot)
```

---

## 8. Stack completo desplegado

```
docker compose up --build -d
```

| Contenedor | Imagen | Puerto | Descripción |
|---|---|---|---|
| `validacion-frontend` | Nginx Alpine | `3000:80` | React SPA + reverse proxy |
| `validacion-app` | Eclipse Temurin 21 JRE | `8080:8080` | Spring Boot API |
| `validacion-postgres` | PostgreSQL 16 | `5432:5432` | Base de datos |

**URLs de acceso:**

| Recurso | URL |
|---|---|
| Frontend React | http://localhost:3000 |
| Portal web Thymeleaf | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |

---

## 9. Commits generados

| Hash | Descripción |
|---|---|
| `bd54362` | fix: corregir bugs, mejorar excepciones y arquitectura hexagonal |
| `7baf56a` | fix: downgrade Gradle a 8.13 y deshabilitar health indicator de mail |
| `e12fcc1` | fix: configurar Jackson para serializar fechas en formato ISO (no timestamps) |
| `b183ab3` | feat: agregar despliegue Docker del frontend React |
| `ed2df93` | docs: agregar CAMBIOS.md con resumen detallado de todas las modificaciones |
| `9c8c814` | fix: corregir puerto del proxy de Vite (8081 → 8080) |

---

## 10. Configuración del entorno de desarrollo frontend

### 10.1 Instalación de dependencias
**Directorio:** `frontend/`
Las dependencias de Node no estaban instaladas localmente. Se ejecutó `npm install` para generar `node_modules/` con todas las dependencias declaradas en `package.json` (React 19, Vite 8, Axios, React Router, etc.).

### 10.2 Corrección del proxy de Vite
**Archivo:** `frontend/vite.config.js`
**Problema:** El proxy del servidor de desarrollo apuntaba a `http://localhost:8081`, pero el backend Spring Boot corre en el puerto **8080**.
**Corrección:**
```js
// Antes
target: 'http://localhost:8081'

// Después
target: 'http://localhost:8080'
```
Vite detectó el cambio automáticamente y reinició el servidor sin intervención manual.

### 10.3 Servidor de desarrollo activo
```
npm run dev
```
- **URL:** http://localhost:5173
- Hot Module Replacement (HMR) activo
- Proxy `/api/*` → `http://localhost:8080` (backend en Docker)
