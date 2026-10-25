-- ============================================================
-- Datos iniciales mínimos (seed) — Semana 6, corregido 27/09/2026
-- ============================================================
INSERT INTO zona (nombre, sector) VALUES
    ('Piura', NULL), ('Castilla', NULL), ('Veintiséis de Octubre', NULL),
    ('Catacaos', NULL), ('La Arena', NULL), ('La Unión', NULL);

INSERT INTO categoria (nombre, activo) VALUES
    ('Bache en la vía', TRUE),
    ('Grieta o hundimiento en la pista', TRUE),
    ('Vereda dañada', TRUE),
    ('Luminaria dañada', TRUE),
    ('Semáforo o señalización dañada', TRUE),
    ('Fuga de agua o desagüe colapsado', TRUE),
    ('Acumulación de basura', TRUE),
    ('Árbol caído o área verde en riesgo', TRUE),
    ('Cables expuestos o poste inclinado', TRUE),
    ('Otra', TRUE);

-- Contraseña de ejemplo (hash simulado, académico): cambiar antes de cualquier uso real.
INSERT INTO usuario_municipal (nombre, usuario, contrasena_hash, rol) VALUES
    ('Administrador PiuraVigila', 'admin', 'HASH_PENDIENTE_DE_IMPLEMENTACION', 'administrador'),
    ('Operador PiuraVigila', 'operador', 'HASH_PENDIENTE_DE_IMPLEMENTACION', 'operador');


