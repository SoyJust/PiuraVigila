-- ============================================================
-- Consultas del dashboard (Semana 7) — sin fotos, solo conteos, promedios y porcentajes.
-- :desde y :hasta son el periodo elegido en pantalla (fecha_registro >= :desde y < :hasta).
-- ============================================================

-- R01 · Zonas críticas (RF-04): reportes por zona. Incluye los vinculados porque miden la demanda real.
SELECT z.nombre AS zona, COUNT(*) AS reportes
FROM reporte r
JOIN zona z ON z.zona_id = r.zona_id
WHERE r.fecha_registro >= :desde AND r.fecha_registro < :hasta
GROUP BY z.nombre
ORDER BY reportes DESC;

-- R02 · Tiempo de atención por categoría (RF-03): días entre el registro y el estado Atendido.
-- Solo casos originales ya Atendidos (el estado solo avanza, por eso hay una fila 'Atendido' por caso).
SELECT c.nombre AS categoria,
       COUNT(*) AS casos_atendidos,
       ROUND(AVG(EXTRACT(EPOCH FROM (h.fecha_cambio - r.fecha_registro)) / 86400)::numeric, 1) AS dias_promedio
FROM reporte r
JOIN categoria c ON c.categoria_id = r.categoria_id
JOIN historial_estado h ON h.reporte_id = r.reporte_id AND h.estado_nuevo = 'Atendido'
WHERE r.reporte_original_id IS NULL
  AND r.fecha_registro >= :desde AND r.fecha_registro < :hasta
GROUP BY c.nombre
ORDER BY dias_promedio DESC;

-- R03 · Resumen de gestión (RF-01, RF-03, RF-05, RF-07)
-- a) Casos por estado (solo casos originales)
SELECT estado, COUNT(*) AS casos
FROM reporte
WHERE reporte_original_id IS NULL AND fecha_registro >= :desde AND fecha_registro < :hasta
GROUP BY estado;

-- b) Duplicados evitados (reportes vinculados a un caso existente)
SELECT COUNT(*) AS duplicados_evitados
FROM reporte
WHERE reporte_original_id IS NOT NULL AND fecha_registro >= :desde AND fecha_registro < :hasta;

-- c) % de casos con estado actualizado (ya salieron de Recibido)
SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE estado <> 'Recibido') / NULLIF(COUNT(*), 0), 1) AS pct_actualizados
FROM reporte
WHERE reporte_original_id IS NULL AND fecha_registro >= :desde AND fecha_registro < :hasta;

-- d) % de reportes «Otra» del mes calendario (se compara con el umbral de 10 %, RF-07)
SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE c.nombre = 'Otra') / NULLIF(COUNT(*), 0), 1) AS pct_otra
FROM reporte r
JOIN categoria c ON c.categoria_id = r.categoria_id
WHERE date_trunc('month', r.fecha_registro) = date_trunc('month', CURRENT_DATE);
