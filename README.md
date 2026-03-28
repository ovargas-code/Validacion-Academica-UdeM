# 🎓 Validación Académica MS

Microservicio Spring Boot para validar información académica, generar certificados PDF y enviarlos por correo.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-green?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-blue?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-purple?style=flat-square)
![H2](https://img.shields.io/badge/H2-gray?style=flat-square)

---

## 🧠 Arquitectura del sistema

### 🔎 Descripción

- Frontend implícito: acceso vía navegador (Swagger o portal)
- Backend: Spring Boot (lógica de validación)
- Generador PDF automático
- Envío de correos con Gmail SMTP

**Base de datos:**
- **H2** → pruebas rápidas
- **PostgreSQL** → Docker

---

## 📋 Requisitos

- Java 21
- Docker Desktop
- PowerShell en Windows

---

## 📧 Configuración del correo

### Crear `.env`

```powershell
Copy-Item .env.example .env
```

Editar `.env` con los siguientes valores:

```env
ADMIN_USER=admin
ADMIN_PASSWORD=udem2026
SERVER_PORT=8081

DB_URL=jdbc:postgresql://localhost:5432/validacion_academica
DB_USERNAME=postgres
DB_PASSWORD=postgres

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=su_correo@gmail.com
MAIL_PASSWORD=su_app_password
MAIL_STARTTLS_ENABLE=true
MAIL_STARTTLS_REQUIRED=false
MAIL_SSL_ENABLE=false
MAIL_DEBUG=true
```

---

## ⚙️ Modos de ejecución

### 🔹 Modo 1 — Local (H2)

```powershell
.\gradlew.bat bootRun --args="--server.port=8081"
```

**Acceso:**
- http://localhost:8081
- http://localhost:8081/h2-console
- http://localhost:8081/swagger-ui/index.html

---
```powershell
cd C:\Users\ovargas\IdeaProjects\frontend
```

```powershell
npm run dev
```
---

### 🔹 Modo 2 — PostgreSQL en Docker + App local

**Limpieza:**

```powershell
docker stop validacion-app validacion-postgres
```

```powershell
docker rm -f validacion-app validacion-postgres
```

```powershell
docker-compose down --remove-orphans
```

**Levantar base de datos:**

```powershell
docker-compose up postgres -d
```

**Verificar contenedores:**

```powershell
docker ps
```

**Ejecutar aplicación:**

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres --server.port=8081"
```

**Acceso:**
- http://localhost:8081
- http://localhost:8081/swagger-ui/index.html

---

---
```powershell
cd C:\Users\ovargas\IdeaProjects\frontend
```

```powershell
npm run dev
```
---

### 🔹 Modo 3 — Todo en Docker

**Limpieza:**

```powershell
docker stop validacion-app validacion-postgres
```

```powershell
docker rm -f validacion-app validacion-postgres
```

```powershell
docker-compose down --remove-orphans
```

**Build y ejecución:**

```powershell
docker-compose up --build
```

**Acceso:**
- http://localhost:8080
- http://localhost:8080/swagger-ui/index.html
- http://localhost:3000/

---
```powershell
cd C:\Users\ovargas\IdeaProjects\frontend
```

```powershell
npm run dev
```
---

> ⚠️ **En Docker el puerto es 8080**

---

## 📬 Flujo funcional

1. Usuario solicita validación
2. Consulta base de datos
3. Resultado **VALID** / **INVALID**
4. Generación PDF
5. Envío de correo

---

## 🧪 Pruebas

- Use registros válidos para generar **VALID**
- Verifique logs SMTP

### Verificación del correo

| Estado | Resultado |
|--------|-----------|
| ✔️ Correo enviado | Éxito |
| ❌ Error SMTP | Fallo |

### Logs esperados

```log
Preparando envío SMTP...
Certificado enviado correctamente
```

---

## 🛠️ Diagnóstico

**Ver puerto ocupado:**

```powershell
netstat -ano | findstr :8080
```

**Ver contenedores:**

```powershell
docker ps -a
```

**Ver logs de PostgreSQL:**

```powershell
docker logs validacion-postgres
```