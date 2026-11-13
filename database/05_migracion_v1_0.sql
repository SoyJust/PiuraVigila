-- ============================================================
-- PiuraVigila · Migración a la versión 1.0/1.1 (acabado, 12 y 13/11/2026)
-- Para una base que ya tiene 01_schema.sql y 02_seed.sql de versiones anteriores.
-- Se puede ejecutar más de una vez (no duplica nada).
-- Uso: psql -U postgres -d piuravigila -f database/05_migracion_v1_0.sql
-- ============================================================
SET client_encoding = 'UTF8';

-- Gestión de usuarios: desactivar en lugar de borrar.
ALTER TABLE usuario_municipal ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

-- v1.1 · comentario opcional al cambiar de estado (lo ve el vecino en la consulta).
ALTER TABLE historial_estado ADD COLUMN IF NOT EXISTS comentario VARCHAR(200);

-- Zonas: los 10 distritos de la provincia de Piura (antes había 6).
INSERT INTO zona (nombre, sector) VALUES
    ('Cura Mori', NULL), ('El Tallán', NULL), ('Las Lomas', NULL), ('Tambo Grande', NULL)
ON CONFLICT (nombre) DO NOTHING;

SELECT zona_id, nombre FROM zona ORDER BY zona_id;
