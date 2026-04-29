# Validación Académica — Universidad de Medellín

Sistema para solicitar y verificar validaciones académicas, con backend Spring Boot/Kotlin, frontend React/Vite, base de datos H2 o PostgreSQL, Swagger y Docker.

## Requisitos

- Java 21
- Node.js
- Docker Desktop
- Git
- PostgreSQL solo si se usa modo Docker/PostgreSQL

## Modo 1 — Desarrollo Local con H2

Backend:

```powershell
.\gradlew.bat bootRun --args="--server.port=8081"
```

Frontend:

```powershell
cd frontend
npm install
npm run dev -- --port 5173 --strictPort false
```

Accesos:

- Backend: [http://localhost:8081](http://localhost:8081)
- Swagger: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- H2 Console: [http://localhost:8081/h2-console](http://localhost:8081/h2-console)
- Frontend Vite: [http://localhost:5173](http://localhost:5173)

## Modo 2 — Todo en Docker

```powershell
docker compose down
docker compose build --no-cache
docker compose up
```

Accesos:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend: [http://localhost:8080](http://localhost:8080)
- Swagger: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- PostgreSQL: `localhost:5432`

## Comandos Útiles

Ver contenedores:

```powershell
docker ps
```

Ver logs backend:

```powershell
docker logs -f validacion-app
```

Ver logs frontend:

```powershell
docker logs -f validacion-frontend
```

Ver logs PostgreSQL:

```powershell
docker logs -f validacion-postgres
```

Ver puerto ocupado:

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :3000
```

## Notas Importantes

- En modo local el backend usa `8081`.
- En Docker el backend usa `8080`.
- El frontend en desarrollo usa Vite en `5173`.
- El frontend en Docker usa `3000`.
- No subir el archivo `.env` real a Git.
- Si Docker muestra una versión vieja, usar `docker compose build --no-cache`.
- La carta de autorización PDF ya no se carga.
- El certificado final del sistema sí se conserva como resultado.


