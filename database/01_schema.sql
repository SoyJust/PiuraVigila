-- Semana 6 · Diseño físico v0.5 (rev. 15/10/2026)
-- Curso Integrador I: Sistemas - Software
-- Proyecto: PiuraVigila (registro y atención de reportes de incidencias urbanas)
--
-- Motor: PostgreSQL 15+ (decisión del equipo, 27/09/2026).
-- Se eligió por: CHECK y BOOLEAN nativos sin configuración adicional (a diferencia
-- de versiones antiguas de MySQL), buena integración con JDBC para la arquitectura
-- Java de Semana 9, y es gratuito. Si en Semana 9 deciden usar MySQL en su lugar,
-- solo hay que revisar: BOOLEAN -> TINYINT(1) (no requiere cambio real),
-- GENERATED ALWAYS AS IDENTITY -> AUTO_INCREMENT en cada PK de abajo, el índice
-- parcial de duplicados (en MySQL sería un índice normal) y la secuencia del código.
--
-- Cambios de la revisión del 15/10/2026 (observaciones del docente):
--  * Se elimina hash_foto: los duplicados ya no se detectan comparando fotos.
--  * Se agrega reporte_original_id: un reporte vinculado apunta al caso original (RF-05, RN-01).
--  * El formato del código se valida con CHECK y el número sale de una secuencia (RNF-03).

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

-- Código de seguimiento (RNF-03): el número lo entrega la base de datos, así que dos envíos
-- simultáneos nunca reciben el mismo. La aplicación arma el código con:
--   'PR-' || EXTRACT(YEAR FROM CURRENT_DATE) || '-' || LPAD(nextval('seq_codigo_reporte')::text, 4, '0')
CREATE SEQUENCE seq_codigo_reporte START 1;

-- Tabla transaccional central
--  * Caso original: reporte_original_id es NULL y tiene su propio código de seguimiento.
--  * Reporte vinculado (RN-01): reporte_original_id apunta al caso original y NO tiene código
--    propio; el ciudadano usa el código del original. Su estado no se usa: manda el del original.
--  * La aplicación solo permite vincular a un caso original abierto (Recibido o En proceso).

CREATE TABLE reporte (
    reporte_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_seguimiento VARCHAR(20) UNIQUE,
    reporte_original_id INT,
    categoria_id INT NOT NULL,
    zona_id INT NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    foto_url VARCHAR(255) NOT NULL, -- solo la ruta de la foto (una por reporte, máx. 2 MB validado por la aplicación)
    lat DECIMAL(9,6),
    lng DECIMAL(9,6),
    estado VARCHAR(20) NOT NULL DEFAULT 'Recibido',
    dispositivo_id VARCHAR(64) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reporte_categoria FOREIGN KEY (categoria_id)
        REFERENCES categoria(categoria_id),
    CONSTRAINT fk_reporte_zona FOREIGN KEY (zona_id)
        REFERENCES zona(zona_id),
    CONSTRAINT fk_reporte_original FOREIGN KEY (reporte_original_id)
        REFERENCES reporte(reporte_id),
    CONSTRAINT ck_reporte_estado CHECK (estado IN ('Recibido','En proceso','Atendido')),
    -- Original: código con formato PR-AAAA-####. Vinculado: sin código propio.
    CONSTRAINT ck_reporte_codigo CHECK (
        (reporte_original_id IS NULL AND codigo_seguimiento IS NOT NULL
            AND codigo_seguimiento ~ '^PR-[0-9]{4}-[0-9]{4}$')
        OR (reporte_original_id IS NOT NULL AND codigo_seguimiento IS NULL)
    ),
    CONSTRAINT ck_reporte_no_autovinculo CHECK (reporte_original_id <> reporte_id)
);

CREATE INDEX idx_reporte_duplicado ON reporte (categoria_id, zona_id, estado) WHERE reporte_original_id IS NULL; -- soporta RF-05 (buscar un caso abierto con la misma categoría y zona)
CREATE INDEX idx_reporte_original ON reporte (reporte_original_id); -- soporta RN-01 (contar reportes vinculados a un caso)
CREATE INDEX idx_reporte_dispositivo_fecha ON reporte (dispositivo_id, fecha_registro); -- soporta RF-06 (intervalo de 60 s y límite diario)

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
--    dispositivo_id no debe asociarse a datos personales.
-- 3. reporte.estado y historial_estado.estado_nuevo usan el mismo CHECK para
--    evitar valores inconsistentes entre el estado actual y el historial.
-- 4. Usar consultas parametrizadas en la etapa de desarrollo (prevención de inyección SQL).
-- 5. Las reglas anti-spam (60 s entre reportes, 3 por día, verificación) y la regla de
--    duplicados se validan en el servidor, no solo en el formulario (RF-06, RN-01).
-- 6. rol controla el acceso dentro de la aplicación (RNF-01); la validación de
--    permisos ocurre en la capa de aplicación, no solo en la base de datos.


