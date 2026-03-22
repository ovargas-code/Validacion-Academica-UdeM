# ============================================================
# ETAPA 1: Construcción
# Usamos una imagen con JDK completo para compilar el proyecto
# ============================================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copiamos primero solo los archivos de configuración de Gradle
# (esto permite que Docker cachee las dependencias si no cambian)
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./

# Damos permisos de ejecución al wrapper de Gradle
RUN chmod +x gradlew

# Descargamos dependencias (este paso se cachea si build.gradle.kts no cambia)
RUN ./gradlew dependencies --no-daemon

# Copiamos el resto del código fuente
COPY src/ src/

# Compilamos y empaquetamos, omitiendo los tests (los tests corren en CI/CD)
RUN ./gradlew bootJar --no-daemon -x test

# ============================================================
# ETAPA 2: Imagen final (mucho más pequeña, solo JRE)
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamos solo el JAR generado en la etapa anterior
COPY --from=builder /app/build/libs/*.jar app.jar

# Puerto que expone la aplicación
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]
