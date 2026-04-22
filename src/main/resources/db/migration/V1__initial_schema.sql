-- =============================================================
-- V1: Schema inicial — validacion-academica-ms
-- Cubre las 5 entidades JPA activas al momento de introducir
-- Flyway. Los tipos y longitudes reflejan exactamente lo que
-- Hibernate genera con SpringPhysicalNamingStrategy para que
-- ddl-auto: validate no falle al arrancar.
-- =============================================================

-- -------------------------------------------------------------
-- 1. students
-- -------------------------------------------------------------
CREATE TABLE students (
    id            BIGSERIAL     PRIMARY KEY,
    document      VARCHAR(20)   NOT NULL,
    full_name     VARCHAR(150)  NOT NULL,
    program       VARCHAR(200)  NOT NULL,
    academic_level VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    degree_title  VARCHAR(200),
    graduation_date DATE
);

CREATE UNIQUE INDEX idx_students_document ON students (document);
CREATE        INDEX idx_students_status   ON students (status);

-- -------------------------------------------------------------
-- 2. validation_requests
-- -------------------------------------------------------------
CREATE TABLE validation_requests (
    id                BIGSERIAL    PRIMARY KEY,
    requester_name    VARCHAR(150) NOT NULL,
    requester_email   VARCHAR(254) NOT NULL,
    student_document  VARCHAR(20)  NOT NULL,
    validation_type   VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    verification_code VARCHAR(20)  NOT NULL
);

CREATE        INDEX idx_validation_student_document   ON validation_requests (student_document);
CREATE UNIQUE INDEX idx_validation_verification_code  ON validation_requests (verification_code);

-- -------------------------------------------------------------
-- 3. email_verification_codes
--    FK: validation_request_id → validation_requests(id)
-- -------------------------------------------------------------
CREATE TABLE email_verification_codes (
    id                    BIGSERIAL    PRIMARY KEY,
    token                 VARCHAR(36)  NOT NULL,
    email                 VARCHAR(254) NOT NULL,
    code                  VARCHAR(6)   NOT NULL,
    validation_request_id BIGINT       NOT NULL
        REFERENCES validation_requests (id) ON DELETE NO ACTION,
    student_document      VARCHAR(20)  NOT NULL,
    -- SMELL: falta length=20 en @Column de EmailVerificationEntity.validationType.
    -- Hibernate defaultea VARCHAR(255) cuando no se especifica length en @Enumerated(STRING).
    -- Deuda menor: normalizar a VARCHAR(20) en migración futura junto con el ajuste en la entidad.
    validation_type       VARCHAR(255) NOT NULL,
    requester_name        VARCHAR(150) NOT NULL,
    expires_at            TIMESTAMP    NOT NULL,
    created_at            TIMESTAMP    NOT NULL,
    used                  BOOLEAN      NOT NULL
);

CREATE UNIQUE INDEX idx_email_verification_token ON email_verification_codes (token);

-- -------------------------------------------------------------
-- 4. audit_events
-- -------------------------------------------------------------
CREATE TABLE audit_events (
    id               BIGSERIAL    PRIMARY KEY,
    action           VARCHAR(30)  NOT NULL,
    performed_by     VARCHAR(100) NOT NULL,
    target_document  VARCHAR(20),
    details          VARCHAR(500),
    timestamp        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_audit_performed_by ON audit_events (performed_by);
CREATE INDEX idx_audit_timestamp    ON audit_events (timestamp);
CREATE INDEX idx_audit_action       ON audit_events (action);

-- -------------------------------------------------------------
-- 5. solicitudes_empresa
-- -------------------------------------------------------------
CREATE TABLE solicitudes_empresa (
    id                        BIGSERIAL    PRIMARY KEY,
    numero_solicitud          VARCHAR(25)  NOT NULL,
    nombre_empresa            VARCHAR(200) NOT NULL,
    nit_empresa               VARCHAR(20)  NOT NULL,
    nombre_contacto           VARCHAR(150) NOT NULL,
    cargo_contacto            VARCHAR(100) NOT NULL,
    correo_contacto           VARCHAR(254) NOT NULL,
    telefono_contacto         VARCHAR(20),
    tipo_documento_estudiante VARCHAR(10)  NOT NULL,
    documento_estudiante      VARCHAR(20)  NOT NULL,
    nombre_estudiante         VARCHAR(150) NOT NULL,
    programa_consultado       VARCHAR(200),
    tipo_validacion           VARCHAR(20)  NOT NULL,
    observaciones             VARCHAR(500),
    acepta_terminos           BOOLEAN      NOT NULL,
    ruta_carta                VARCHAR(500) NOT NULL,
    estado                    VARCHAR(20)  NOT NULL,
    created_at                TIMESTAMP    NOT NULL,
    updated_at                TIMESTAMP
);

CREATE UNIQUE INDEX idx_solicitud_numero               ON solicitudes_empresa (numero_solicitud);
CREATE        INDEX idx_solicitud_created_at           ON solicitudes_empresa (created_at);
CREATE        INDEX idx_solicitud_documento_estudiante ON solicitudes_empresa (documento_estudiante);
