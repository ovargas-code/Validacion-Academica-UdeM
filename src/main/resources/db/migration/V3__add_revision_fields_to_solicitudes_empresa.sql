-- =============================================================
-- V3: Campos de revisión administrativa en solicitudes_empresa
-- Todos nullable — las solicitudes existentes no tienen revisión.
-- =============================================================

ALTER TABLE solicitudes_empresa ADD COLUMN comentario_admin VARCHAR(1000);
ALTER TABLE solicitudes_empresa ADD COLUMN fecha_revision TIMESTAMP;
ALTER TABLE solicitudes_empresa ADD COLUMN admin_responsable VARCHAR(100);
