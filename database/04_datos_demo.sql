-- ============================================================
-- PiuraVigila · Datos de demostración (ficticios)
-- 20 casos + 8 reportes vinculados repartidos en las 6 zonas, con estados,
-- historial y coordenadas aproximadas de Piura. Las fechas son relativas a HOY,
-- así que el mapa y el panel se ven "vivos" en cualquier momento.
--
-- Requisitos: haber cargado 01_schema.sql y 02_seed.sql (zonas con distritos reales).
-- Uso:        psql -U postgres -d piuravigila -f database/04_datos_demo.sql
-- Se puede ejecutar UNA sola vez (si ya hay datos demo, no inserta nada).
-- Todo lo demo se reconoce por dispositivo_id = 'demo-...'. Para quitarlo, ver el final.
-- ============================================================
SET client_encoding = 'UTF8';

-- Funciones temporales: desaparecen al cerrar la sesión, no quedan en la base.
CREATE FUNCTION pg_temp.demo_caso(
    p_cat text, p_zona text, p_desc text, p_lat numeric, p_lng numeric,
    p_dias numeric, p_estado text, p_d_proceso numeric, p_d_atendido numeric,
    p_disp text, p_foto text) RETURNS int
LANGUAGE plpgsql AS $$
DECLARE
    v_cat int; v_zona int; v_id int; v_op int; v_adm int; v_resp int;
    v_ahora timestamp := (now() AT TIME ZONE 'America/Lima');
    v_fecha timestamp;
BEGIN
    SELECT categoria_id INTO v_cat FROM categoria WHERE nombre LIKE (p_cat || '%') ORDER BY categoria_id LIMIT 1;
    SELECT zona_id INTO v_zona FROM zona WHERE nombre LIKE (p_zona || '%') ORDER BY zona_id LIMIT 1;
    IF v_cat IS NULL THEN RAISE EXCEPTION 'No existe la categoria que empieza con "%"', p_cat; END IF;
    IF v_zona IS NULL THEN RAISE EXCEPTION 'No existe la zona "%": primero actualiza los nombres de las zonas', p_zona; END IF;
    SELECT usuario_id INTO v_op  FROM usuario_municipal WHERE usuario = 'operador';
    SELECT usuario_id INTO v_adm FROM usuario_municipal WHERE usuario = 'admin';
    IF v_op IS NULL AND v_adm IS NULL THEN RAISE EXCEPTION 'No hay usuarios municipales (admin/operador)'; END IF;

    v_fecha := date_trunc('minute', v_ahora - make_interval(secs => (p_dias * 86400)::float8));

    INSERT INTO reporte(codigo_seguimiento, categoria_id, zona_id, descripcion, foto_url, lat, lng, estado, dispositivo_id, fecha_registro)
    VALUES ('PR-' || EXTRACT(YEAR FROM v_fecha)::int || '-' || LPAD(nextval('seq_codigo_reporte')::text, 4, '0'),
            v_cat, v_zona, p_desc, p_foto, p_lat, p_lng, p_estado, p_disp, v_fecha)
    RETURNING reporte_id INTO v_id;

    -- Igual que la app: el "Recibido" automático no tiene responsable.
    INSERT INTO historial_estado(reporte_id, usuario_id, estado_nuevo, fecha_cambio)
    VALUES (v_id, NULL, 'Recibido', v_fecha);

    IF p_estado IN ('En proceso', 'Atendido') THEN
        v_resp := COALESCE(CASE WHEN v_id % 2 = 0 THEN v_op ELSE v_adm END, v_op, v_adm);
        INSERT INTO historial_estado(reporte_id, usuario_id, estado_nuevo, fecha_cambio)
        VALUES (v_id, v_resp, 'En proceso',
                LEAST(date_trunc('minute', v_fecha + make_interval(secs => (p_d_proceso * 86400)::float8)), v_ahora));
    END IF;
    IF p_estado = 'Atendido' THEN
        v_resp := COALESCE(v_op, v_adm);
        INSERT INTO historial_estado(reporte_id, usuario_id, estado_nuevo, fecha_cambio)
        VALUES (v_id, v_resp, 'Atendido',
                LEAST(date_trunc('minute', v_fecha + make_interval(secs => (p_d_atendido * 86400)::float8)), v_ahora));
    END IF;
    RETURN v_id;
END $$;

-- Reporte vinculado a un caso abierto: hereda categoría y zona, sin código propio (RN-01).
CREATE FUNCTION pg_temp.demo_vinculado(
    p_original int, p_desc text, p_lat numeric, p_lng numeric,
    p_dias numeric, p_disp text, p_foto text) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    v_fecha timestamp := date_trunc('minute',
        (now() AT TIME ZONE 'America/Lima') - make_interval(secs => (p_dias * 86400)::float8));
BEGIN
    INSERT INTO reporte(reporte_original_id, categoria_id, zona_id, descripcion, foto_url, lat, lng, estado, dispositivo_id, fecha_registro)
    SELECT reporte_id, categoria_id, zona_id, p_desc, p_foto, p_lat, p_lng, 'Recibido', p_disp, v_fecha
    FROM reporte WHERE reporte_id = p_original;
END $$;

-- Todo en un solo bloque: si algo falla, no se inserta nada.
DO $$
DECLARE
    c1 int; c2 int; c3 int; c4 int; c6 int; c10 int; c12 int;
BEGIN
    IF EXISTS (SELECT 1 FROM reporte WHERE dispositivo_id LIKE 'demo-%') THEN
        RAISE EXCEPTION 'Los datos de demostracion ya estan cargados. No se inserto nada.';
    END IF;

    -- ---- Casos antiguos (se insertan del más viejo al más nuevo) ----
    PERFORM pg_temp.demo_caso('Bache', 'Castilla',
        'Bache en el cruce frente al grifo. Ya lo taparon antes con tierra y volvió a abrirse; los carros frenan de golpe para esquivarlo.',
        -5.1977, -80.6168, 71.2, 'Atendido', 6, 15, 'demo-d01', 'fotos/demo-01.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'La Arena',
        'Las luminarias de la losa deportiva y de la calle de al lado están apagadas hace semanas. En la noche no se puede transitar con tranquilidad.',
        -5.3412, -80.7121, 58.0, 'Atendido', 10, 21, 'demo-d02', 'fotos/demo-02.jpg');
    PERFORM pg_temp.demo_caso('_rbol', 'Veintis',
        'Rama muy grande a punto de caer sobre el cableado y la vereda del parque. Piden poda urgente antes de que ocurra un accidente.',
        -5.1790, -80.6510, 47.5, 'Atendido', 8, 16, 'demo-d03', 'fotos/demo-03.jpg');
    PERFORM pg_temp.demo_caso('Fuga', 'Castilla',
        'Tubería rota en la esquina; sale agua de día y de noche y se desperdicia muchísimo. Ya hay lodo en toda la cuadra.',
        -5.2062, -80.6096, 38.0, 'Atendido', 1, 4, 'demo-d04', 'fotos/demo-04.jpg');

    -- ---- Últimas semanas ----
    PERFORM pg_temp.demo_caso('Vereda', 'Piura',
        'Vereda rota frente al mercado, con tramos hundidos. Los adultos mayores y las personas con bastón tienen dificultad para pasar.',
        -5.2011, -80.6309, 27.3, 'Atendido', 6, 14, 'demo-d05', 'fotos/demo-05.jpg');
    PERFORM pg_temp.demo_caso('Acumulaci', 'La Uni',
        'Botadero informal a un costado de la carretera. Acumulan desmonte y residuos, y a veces los queman; el humo llega hasta las casas.',
        -5.4003, -80.7347, 24.6, 'Recibido', NULL, NULL, 'demo-d06', 'fotos/demo-06.jpg');
    PERFORM pg_temp.demo_caso('Grieta', 'Piura',
        'Hundimiento de la pista en la esquina. Se abrió una grieta que crece cada semana con el paso de camiones pesados.',
        -5.1976, -80.6240, 21.0, 'Atendido', 5, 12, 'demo-d07', 'fotos/demo-07.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Catacaos',
        'Alumbrado público apagado en toda la calle de la iglesia. Los vecinos piden revisar el circuito porque ya van varias noches a oscuras.',
        -5.2681, -80.6762, 17.4, 'Atendido', 2, 5, 'demo-d08', 'fotos/demo-08.jpg');
    c12 := pg_temp.demo_caso('Fuga', 'La Arena',
        'Desagüe colapsado frente a las viviendas: aguas servidas corriendo por la calle. Los niños pasan por ahí para ir al colegio.',
        -5.3398, -80.7092, 14.7, 'En proceso', 4, NULL, 'demo-d09', 'fotos/demo-09.jpg');
    PERFORM pg_temp.demo_vinculado(c12,
        'Es el mismo desagüe. El olor ya es insoportable y hay mosquitos en toda la cuadra.',
        -5.3401, -80.7095, 12.0, 'demo-d10', 'fotos/demo-10.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'Veintis',
        'Varios baches seguidos en la avenida; el pavimento se rompió después de las lluvias y los mototaxis se malogran las llantas.',
        -5.1705, -80.6612, 12.2, 'Atendido', 3, 7, 'demo-d11', 'fotos/demo-11.jpg');
    c10 := pg_temp.demo_caso('Cables', 'Piura',
        'Poste inclinado con cables colgando a baja altura sobre la vereda, cerca de un paradero. Hay riesgo de accidente si pasa un camión alto.',
        -5.1912, -80.6355, 9.5, 'En proceso', 2, NULL, 'demo-d12', 'fotos/demo-12.jpg');
    PERFORM pg_temp.demo_vinculado(c10,
        'Confirmo: los cables siguen bajos y el poste cada vez más inclinado.',
        -5.1914, -80.6352, 8.0, 'demo-d13', 'fotos/demo-13.jpg');

    -- ---- Últimos 7 días ----
    PERFORM pg_temp.demo_caso('Acumulaci', 'Castilla',
        'El camión recolector no pasa hace tres días y los vecinos sacaron la basura a la esquina. Ya se juntó bastante y hay perros rompiendo las bolsas.',
        -5.1998, -80.6173, 6.3, 'Atendido', 0.5, 3, 'demo-d14', 'fotos/demo-14.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'Catacaos',
        'Hueco grande a la entrada del pueblo, a pocos metros de la plaza de armas. Se malogran las llantas de mototaxis y bicicletas.',
        -5.2664, -80.6741, 5.2, 'Recibido', NULL, NULL, 'demo-d15', 'fotos/demo-15.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Veintis',
        'La luminaria del parque principal quedó apagada tras la lluvia y la loza deportiva está a oscuras. Los jóvenes que practican se quedan sin luz.',
        -5.1768, -80.6584, 4.4, 'Atendido', 0.6, 2.5, 'demo-d16', 'fotos/demo-16.jpg');
    c6 := pg_temp.demo_caso('Sem', 'Piura',
        'El semáforo del cruce principal parpadea en ámbar y no cambia a verde. Se forman colas largas y ya hubo varios roces entre carros.',
        -5.1930, -80.6262, 3.6, 'En proceso', 1.5, NULL, 'demo-d17', 'fotos/demo-17.jpg');
    PERFORM pg_temp.demo_caso('Vereda', 'Castilla',
        'Vereda levantada por las raíces de un árbol. Una señora se tropezó ayer; falta reparar el tramo frente al colegio.',
        -5.2036, -80.6118, 3.1, 'Recibido', NULL, NULL, 'demo-d18', 'fotos/demo-18.jpg');
    c4 := pg_temp.demo_caso('Fuga', 'Piura',
        'Fuga de agua potable en la vereda; corre por la pista desde ayer y forma un charco grande que salpica a los que pasan.',
        -5.1983, -80.6335, 2.3, 'En proceso', 1.1, NULL, 'demo-d19', 'fotos/demo-19.jpg');
    c3 := pg_temp.demo_caso('Acumulaci', 'Veintis',
        'Desmonte y bolsas de basura acumuladas en la esquina hace más de una semana. Ya hay mal olor y moscas cerca de las casas.',
        -5.1742, -80.6528, 1.8, 'En proceso', 0.8, NULL, 'demo-d20', 'fotos/demo-20.jpg');
    c2 := pg_temp.demo_caso('Luminaria', 'Castilla',
        'Tres postes seguidos sin luz en la cuadra del parque. Zona oscura; los vecinos tienen miedo de salir de noche.',
        -5.2018, -80.6142, 1.2, 'Recibido', NULL, NULL, 'demo-d21', 'fotos/demo-21.jpg');
    c1 := pg_temp.demo_caso('Bache', 'Piura',
        'Bache profundo en el carril derecho, cerca del cruce con la avenida principal. Las motos y los carros lo esquivan y es peligroso de noche.',
        -5.1962, -80.6301, 0.4, 'Recibido', NULL, NULL, 'demo-d22', 'fotos/demo-22.jpg');

    -- ---- Reportes de otros vecinos sobre casos que ya estaban abiertos ----
    PERFORM pg_temp.demo_vinculado(c6,
        'Es el mismo semáforo. Hoy en la mañana estuvo igual y la cola llegaba hasta la otra cuadra.',
        -5.1931, -80.6264, 2.5, 'demo-d23', 'fotos/demo-23.jpg');
    PERFORM pg_temp.demo_vinculado(c4,
        'Sigue la fuga, ya perdieron bastante agua. Se ve el chorro desde la esquina.',
        -5.1985, -80.6338, 1.9, 'demo-d24', 'fotos/demo-24.jpg');
    PERFORM pg_temp.demo_vinculado(c3,
        'La basura sigue ahí y ahora también hay desmonte. Pasen pronto, por favor.',
        -5.1744, -80.6531, 1.1, 'demo-d25', 'fotos/demo-25.jpg');
    PERFORM pg_temp.demo_vinculado(c3,
        'Mismo punto de basura. Los vecinos ya no saben qué hacer con el olor.',
        -5.1740, -80.6526, 0.6, 'demo-d26', 'fotos/demo-26.jpg');
    PERFORM pg_temp.demo_vinculado(c2,
        'Sigue sin luz el parque; ya van varios días y se juntan personas sospechosas.',
        -5.2016, -80.6145, 0.8, 'demo-d27', 'fotos/demo-27.jpg');
    PERFORM pg_temp.demo_vinculado(c1,
        'Es el mismo bache. Casi se cae mi moto anoche, ojalá lo reparen pronto.',
        -5.1964, -80.6298, 0.2, 'demo-d28', 'fotos/demo-28.jpg');
END $$;

-- Verificación rápida
SELECT z.nombre AS zona, COUNT(*) AS reportes
FROM reporte r JOIN zona z ON z.zona_id = r.zona_id
WHERE r.dispositivo_id LIKE 'demo-%'
GROUP BY z.nombre ORDER BY reportes DESC;

-- ------------------------------------------------------------
-- PARA QUITAR LOS DATOS DE DEMO (ejecutar a mano, solo si lo necesitas):
--   DELETE FROM historial_estado WHERE reporte_id IN (SELECT reporte_id FROM reporte WHERE dispositivo_id LIKE 'demo-%');
--   DELETE FROM reporte WHERE reporte_original_id IN (SELECT reporte_id FROM reporte WHERE dispositivo_id LIKE 'demo-%');
--   DELETE FROM reporte WHERE dispositivo_id LIKE 'demo-%';
-- ------------------------------------------------------------
