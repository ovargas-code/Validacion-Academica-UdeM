plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "co.edu.udemedellin"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- NÚCLEO WEB Y VALIDACIÓN ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // --- FRONTEND Y SEGURIDAD (LAS NUEVAS) ---
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // JWT

    // --- RATE LIMITING ---
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // --- KOTLIN Y JSON ---
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // --- DOCUMENTACIÓN (SWAGGER) ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // --- PDF Y QR ---
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.google.zxing:javase:3.5.2")

    // --- CORREO ---
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // --- BASES DE DATOS ---
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // --- PRUEBAS ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Carga variables del archivo .env como env vars del proceso bootRun
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val idx = trimmed.indexOf('=')
                if (idx > 0) {
                    val key = trimmed.substring(0, idx).trim()
                    val value = trimmed.substring(idx + 1).trim()
                    environment(key, value)
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}