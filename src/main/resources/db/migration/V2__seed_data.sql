-- =============================================================
-- V2: Seed data inicial — estudiantes de demostración
-- Equivalente al data.sql usado por el perfil H2 (create-drop).
-- ON CONFLICT DO NOTHING protege ante re-ejecuciones accidentales
-- en entornos con flyway.outOfOrder o restauraciones parciales.
-- =============================================================

INSERT INTO students (document, full_name, program, academic_level, status, degree_title, graduation_date)
VALUES ('10350001', 'Ana Gomez', 'Medicina', 'PREGRADO', 'ACTIVO', NULL, NULL)
ON CONFLICT (document) DO NOTHING;

INSERT INTO students (document, full_name, program, academic_level, status, degree_title, graduation_date)
VALUES ('10350002', 'Carlos Perez', 'Ingenieria de Sistemas', 'PREGRADO', 'ACTIVO', NULL, NULL)
ON CONFLICT (document) DO NOTHING;

INSERT INTO students (document, full_name, program, academic_level, status, degree_title, graduation_date)
VALUES ('10350003', 'Maria Torres', 'Derecho', 'ESPECIALIZACION', 'GRADUADO', 'Abogada', '2024-06-15')
ON CONFLICT (document) DO NOTHING;

INSERT INTO students (document, full_name, program, academic_level, status, degree_title, graduation_date)
VALUES ('10350004', 'Jorge Ramirez', 'Administracion de Empresas', 'PREGRADO', 'ACTIVO', NULL, NULL)
ON CONFLICT (document) DO NOTHING;

INSERT INTO students (document, full_name, program, academic_level, status, degree_title, graduation_date)
VALUES ('10350005', 'Sofia Herrera', 'Psicologia', 'MAESTRIA', 'GRADUADO', 'Psicologa Clinica', '2025-11-30')
ON CONFLICT (document) DO NOTHING;
