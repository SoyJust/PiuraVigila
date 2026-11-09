-- Semana 6 · Diseño físico inicial v0.5
-- Curso Integrador I: Sistemas - Software
-- Proyecto: PiuraVigila (registro y atención de reportes de incidencias urbanas)
--
-- Motor: PostgreSQL 15+ (decisión del equipo, 27/09/2026).
-- Se eligió por: CHECK y BOOLEAN nativos sin configuración adicional (a diferencia
-- de versiones antiguas de MySQL), buena integración con JDBC para la arquitectura
-- Java de Semana 9, y es gratuito. Si en Semana 9 deciden usar MySQL en su lugar,
-- solo hay que revisar: BOOLEAN -> TINYINT(1) (no requiere cambio real) y
-- GENERATED ALWAYS AS IDENTITY -> AUTO_INCREMENT en cada PK de abajo.

-- Tablas sin dependencias (se crean primero)

CREATE TABLE zona (
    zona_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    sector VARCHAR(60)
);

CREATE TABLE categoria (
    categoria_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario_municipal (
    usuario_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(40) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('administrador','operador'))
);

-- Tabla transaccional central

CREATE TABLE reporte (
    reporte_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_seguimiento VARCHAR(20) NOT NULL UNIQUE,
    categoria_id INT NOT NULL,
    zona_id INT NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    foto_url VARCHAR(255) NOT NULL,
    hash_foto VARCHAR(64) NOT NULL,
    lat DECIMAL(9,6),
    lng DECIMAL(9,6),
    estado VARCHAR(20) NOT NULL DEFAULT 'Recibido',
    dispositivo_id VARCHAR(64) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reporte_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria(categoria_id),
    CONSTRAINT fk_reporte_zona FOREIGN KEY (zona_id)
        REFERENCES zona(zona_id),
    CONSTRAINT ck_reporte_estado CHECK (estado IN ('Recibido','En proceso','Atendido'))
);

CREATE INDEX idx_reporte_hash_zona ON reporte (hash_foto, zona_id, fecha_registro); -- soporta RF-05 (detección de duplicados)
CREATE INDEX idx_reporte_dispositivo_fecha ON reporte (dispositivo_id, fecha_registro); -- soporta RF-06 (límite diario)

-- Las FK no se indexan automáticamente en Postgres/MySQL; sin estos índices,
-- los joins y agregaciones por categoría/zona hacen sequential scan.
CREATE INDEX idx_reporte_categoria ON reporte (categoria_id); -- soporta RF-03/RF-07 (filtrar/agrupar por categoría)
CREATE INDEX idx_reporte_zona ON reporte (zona_id); -- soporta RF-04 (mapa de zonas críticas, agrupación por zona)

-- Historial de cambios de estado

CREATE TABLE historial_estado (
    historial_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reporte_id INT NOT NULL,
    usuario_id INT,
    estado_nuevo VARCHAR(20) NOT NULL,
    fecha_cambio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_reporte FOREIGN KEY (reporte_id)
        REFERENCES reporte(reporte_id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario_municipal(usuario_id),
    CONSTRAINT ck_historial_estado CHECK (estado_nuevo IN ('Recibido','En proceso','Atendido')),
    -- S03: todo cambio hecho por personal debe traer responsable; el NULL solo se permite
    -- para el 'Recibido' automático que genera el sistema al crear el reporte.
    CONSTRAINT ck_historial_responsable CHECK (usuario_id IS NOT NULL OR estado_nuevo = 'Recibido')
);

CREATE INDEX idx_historial_reporte ON historial_estado (reporte_id); -- soporta RF-02 (consultar historial de un reporte)
CREATE INDEX idx_historial_usuario ON historial_estado (usuario_id); -- soporta RF-03 (auditoría por responsable)

-- Nota: usuario_id es NULL cuando el estado "Recibido" lo genera el sistema
-- automáticamente al registrarse el reporte, no una persona del panel.

-- Decisiones de seguridad documentadas (ver hoja Seguridad, Semana 6):
-- 1. Datos sensibles: usuario_municipal.contrasena_hash (nunca texto plano; solo hash).
-- 2. El reporte es anónimo por diseño: no existe tabla de identidad del ciudadano;
--    hash_foto y dispositivo_id no deben asociarse a datos personales.
-- 3. reporte.estado y historial_estado.estado_nuevo usan el mismo CHECK para
--    evitar valores inconsistentes entre el estado actual y el historial.
-- 4. Usar consultas parametrizadas en la etapa de desarrollo (prevención de inyección SQL).
-- 5. rol controla el acceso dentro de la aplicación (RNF-01); la validación de
--    permisos ocurre en la capa de aplicación, no solo en la base de datos.


-- ============================================================
-- Datos iniciales mínimos (seed) — Semana 6, corregido 27/09/2026
-- ============================================================
INSERT INTO zona (nombre, sector) VALUES
    ('Centro', NULL), ('Norte', NULL), ('Sur', NULL),
    ('Este', NULL), ('Oeste', NULL), ('Castilla', NULL);

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
    ('Administrador PiuraVigila', 'admin', 'HASH_PENDIENTE_DE_IMPLEMENTACION', 'administrador');
