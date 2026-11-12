-- ============================================================
-- PiuraVigila · Datos de demostración (ficticios)
-- 59 casos + 13 reportes vinculados repartidos en los 10 distritos (últimos 5 meses), con estados,
-- historial y coordenadas aproximadas de Piura. Las fechas son relativas a HOY,
-- así que el mapa y el panel se ven "vivos" en cualquier momento.
--
-- Requisitos: haber cargado 01_schema.sql y 02_seed.sql (10 distritos) o, en una base antigua, 05_migracion_v1_0.sql.
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
    e20 int; e24 int; e27 int; e34 int;
BEGIN
    IF EXISTS (SELECT 1 FROM reporte WHERE dispositivo_id LIKE 'demo-%') THEN
        RAISE EXCEPTION 'Los datos de demostracion ya estan cargados. No se inserto nada.';
    END IF;

    -- ---- Todos los casos y reportes vinculados, del más antiguo al más nuevo ----
    -- (así los códigos PR-AAAA-NNNN quedan en orden cronológico)
    PERFORM pg_temp.demo_caso('Bache', 'Piura',
        'Bache en la avenida a la altura del óvalo; se llena de agua cuando llueve y los carros salpican a los peatones.',
        -5.1950, -80.6290, 148.0, 'Atendido', 4, 11, 'demo-e01', 'fotos/demo-e01.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Castilla',
        'Postes apagados en el pasaje que da al puente; los estudiantes regresan de noche por ahí.',
        -5.1985, -80.6200, 141.0, 'Atendido', 7, 18, 'demo-e02', 'fotos/demo-e02.jpg');
    PERFORM pg_temp.demo_caso('Acumulaci', 'Piura',
        'Basura acumulada junto al canal; cuando sopla el viento las bolsas terminan dentro del agua.',
        -5.1880, -80.6420, 135.0, 'Atendido', 2, 6, 'demo-e03', 'fotos/demo-e03.jpg');
    PERFORM pg_temp.demo_caso('Fuga', 'Veintis',
        'Buzón de desagüe destapado y rebalsando en plena calle; el agua llega a la puerta de las casas.',
        -5.1815, -80.6690, 128.0, 'Atendido', 1, 5, 'demo-e04', 'fotos/demo-e04.jpg');
    PERFORM pg_temp.demo_caso('Vereda', 'Catacaos',
        'Vereda partida en la calle de los talleres de artesanía; vecinos y visitantes tropiezan seguido.',
        -5.2668, -80.6772, 122.0, 'Atendido', 9, 24, 'demo-e05', 'fotos/demo-e05.jpg');
    PERFORM pg_temp.demo_caso('Sem', 'Castilla',
        'Señal de PARE caída en el cruce cerca del colegio; los conductores ya no frenan.',
        -5.2041, -80.6150, 117.0, 'Atendido', 3, 8, 'demo-e06', 'fotos/demo-e06.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'Tambo Grande',
        'Huecos en la vía de ingreso a la ciudad; los camiones con fruta pasan despacio y se forman colas.',
        -4.9285, -80.3418, 112.0, 'Atendido', 10, 26, 'demo-e07', 'fotos/demo-e07.jpg');
    PERFORM pg_temp.demo_caso('_rbol', 'Piura',
        'Árbol seco con el tronco rajado en el parque; podría caer sobre las bancas donde se sientan los vecinos.',
        -5.1925, -80.6380, 106.0, 'Atendido', 5, 13, 'demo-e08', 'fotos/demo-e08.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Las Lomas',
        'Calle principal sin alumbrado desde hace semanas; los mototaxistas piden reponer los focos.',
        -4.6555, -80.2430, 99.0, 'Atendido', 12, 30, 'demo-e09', 'fotos/demo-e09.jpg');
    PERFORM pg_temp.demo_caso('Acumulaci', 'Cura Mori',
        'Montículo de basura en la salida hacia Catacaos; por las tardes queman los residuos.',
        -5.3215, -80.6608, 94.0, 'Atendido', 6, 17, 'demo-e10', 'fotos/demo-e10.jpg');
    PERFORM pg_temp.demo_caso('Grieta', 'Castilla',
        'Pista hundida frente al mercado; los camiones de carga la empeoran cada día.',
        -5.2002, -80.6135, 88.0, 'Atendido', 8, 20, 'demo-e11', 'fotos/demo-e11.jpg');
    PERFORM pg_temp.demo_caso('Fuga', 'Piura',
        'Agua potable brotando de la tubería en la berma central; se pierde agua desde el fin de semana.',
        -5.1890, -80.6305, 82.0, 'Atendido', 1, 3, 'demo-e12', 'fotos/demo-e12.jpg');
    PERFORM pg_temp.demo_caso('Cables', 'Veintis',
        'Cable de telefonía cortado colgando sobre la pista; se enreda con los mototaxis al pasar.',
        -5.1760, -80.6630, 76.0, 'Atendido', 2, 9, 'demo-e13', 'fotos/demo-e13.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'Castilla',
        'Bache en el cruce frente al grifo. Ya lo taparon antes con tierra y volvió a abrirse; los carros frenan de golpe para esquivarlo.',
        -5.1977, -80.6168, 71.2, 'Atendido', 6, 15, 'demo-d01', 'fotos/demo-01.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'El Tall',
        'Trocha con baches profundos camino al centro poblado; los vehículos se atascan cuando llueve.',
        -5.4075, -80.6815, 69.0, 'Atendido', 14, 33, 'demo-e14', 'fotos/demo-e14.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Piura',
        'Luminarias apagadas en la cuadra de la universidad; los estudiantes salen tarde y la calle queda a oscuras.',
        -5.1855, -80.6255, 64.0, 'Atendido', 3, 9, 'demo-e15', 'fotos/demo-e15.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'La Arena',
        'Las luminarias de la losa deportiva y de la calle de al lado están apagadas hace semanas. En la noche no se puede transitar con tranquilidad.',
        -5.3412, -80.7121, 58.0, 'Atendido', 10, 21, 'demo-d02', 'fotos/demo-02.jpg');
    PERFORM pg_temp.demo_caso('Acumulaci', 'La Arena',
        'Basura en la orilla del dren; los vecinos piden que pongan un contenedor.',
        -5.3442, -80.7088, 57.0, 'Atendido', 4, 10, 'demo-e16', 'fotos/demo-e16.jpg');
    PERFORM pg_temp.demo_caso('Vereda', 'Veintis',
        'Rampa de la vereda destruida frente a la posta; las sillas de ruedas no pueden subir.',
        -5.1801, -80.6602, 52.0, 'Atendido', 7, 19, 'demo-e17', 'fotos/demo-e17.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'Castilla',
        'Bache al lado del paradero de colectivos; los pasajeros bajan sobre el agua estancada.',
        -5.1969, -80.6082, 49.0, 'Atendido', 5, 12, 'demo-e18', 'fotos/demo-e18.jpg');
    PERFORM pg_temp.demo_caso('_rbol', 'Veintis',
        'Rama muy grande a punto de caer sobre el cableado y la vereda del parque. Piden poda urgente antes de que ocurra un accidente.',
        -5.1790, -80.6510, 47.5, 'Atendido', 8, 16, 'demo-d03', 'fotos/demo-03.jpg');
    PERFORM pg_temp.demo_caso('Sem', 'Piura',
        'Semáforo peatonal sin luz verde en el cruce del hospital; las personas cruzan sin saber cuándo.',
        -5.1938, -80.6398, 45.0, 'Atendido', 1, 4, 'demo-e19', 'fotos/demo-e19.jpg');
    e20 := pg_temp.demo_caso('Fuga', 'Tambo Grande',
        'Desagüe colapsado en la calle del mercado; las aguas negras llegan hasta los puestos de venta.',
        -4.9262, -80.3452, 42.0, 'En proceso', 6, NULL, 'demo-e20', 'fotos/demo-e20.jpg');
    PERFORM pg_temp.demo_caso('_rbol', 'Castilla',
        'Árbol caído tras el viento fuerte; bloquea media pista y los carros pasan por la vereda.',
        -5.2055, -80.6165, 39.0, 'Atendido', 3, 8, 'demo-e21', 'fotos/demo-e21.jpg');
    PERFORM pg_temp.demo_caso('Fuga', 'Castilla',
        'Tubería rota en la esquina; sale agua de día y de noche y se desperdicia muchísimo. Ya hay lodo en toda la cuadra.',
        -5.2062, -80.6096, 38.0, 'Atendido', 1, 4, 'demo-d04', 'fotos/demo-04.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Cura Mori',
        'Plaza sin alumbrado; las familias dejaron de ir por la noche.',
        -5.3195, -80.6635, 35.0, 'Atendido', 9, 22, 'demo-e22', 'fotos/demo-e22.jpg');
    PERFORM pg_temp.demo_caso('Grieta', 'Piura',
        'Grieta larga en la pista de la urbanización; se abre más con cada lluvia.',
        -5.2040, -80.6280, 33.0, 'Atendido', 6, 15, 'demo-e23', 'fotos/demo-e23.jpg');
    PERFORM pg_temp.demo_vinculado(e20,
        'Mismo desagüe del mercado. Los comerciantes ya tuvieron que cerrar dos puestos por el olor.',
        -4.9264, -80.3450, 30.0, 'demo-v01', 'fotos/demo-v01.jpg');
    e24 := pg_temp.demo_caso('Bache', 'Las Lomas',
        'Bache grande en la entrada del pueblo, junto al grifo; ya se cayó un motociclista.',
        -4.6570, -80.2455, 29.0, 'En proceso', 8, NULL, 'demo-e24', 'fotos/demo-e24.jpg');
    PERFORM pg_temp.demo_caso('Vereda', 'Piura',
        'Vereda rota frente al mercado, con tramos hundidos. Los adultos mayores y las personas con bastón tienen dificultad para pasar.',
        -5.2011, -80.6309, 27.3, 'Atendido', 6, 14, 'demo-d05', 'fotos/demo-05.jpg');
    PERFORM pg_temp.demo_caso('Acumulaci', 'Piura',
        'Puntos de basura en la avenida después de la feria del domingo; nadie los recogió.',
        -5.1902, -80.6448, 26.0, 'Atendido', 2, 5, 'demo-e25', 'fotos/demo-e25.jpg');
    PERFORM pg_temp.demo_caso('Acumulaci', 'La Uni',
        'Botadero informal a un costado de la carretera. Acumulan desmonte y residuos, y a veces los queman; el humo llega hasta las casas.',
        -5.4003, -80.7347, 24.6, 'Recibido', NULL, NULL, 'demo-d06', 'fotos/demo-06.jpg');
    PERFORM pg_temp.demo_caso('Cables', 'Castilla',
        'Poste de madera inclinado con el transformador encima, frente al colegio.',
        -5.2025, -80.6105, 23.0, 'Atendido', 3, 7, 'demo-e26', 'fotos/demo-e26.jpg');
    PERFORM pg_temp.demo_caso('Grieta', 'Piura',
        'Hundimiento de la pista en la esquina. Se abrió una grieta que crece cada semana con el paso de camiones pesados.',
        -5.1976, -80.6240, 21.0, 'Atendido', 5, 12, 'demo-d07', 'fotos/demo-07.jpg');
    e27 := pg_temp.demo_caso('Vereda', 'Piura',
        'Vereda hundida frente a la parroquia; se forma un charco que no se seca y hay que pasar por la pista.',
        -5.1958, -80.6345, 20.0, 'En proceso', 6, NULL, 'demo-e27', 'fotos/demo-e27.jpg');
    PERFORM pg_temp.demo_caso('Otra', 'Catacaos',
        'Tapa de buzón robada en la calle principal; los vecinos pusieron una rama como aviso.',
        -5.2649, -80.6730, 18.0, 'Recibido', NULL, NULL, 'demo-e28', 'fotos/demo-e28.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Catacaos',
        'Alumbrado público apagado en toda la calle de la iglesia. Los vecinos piden revisar el circuito porque ya van varias noches a oscuras.',
        -5.2681, -80.6762, 17.4, 'Atendido', 2, 5, 'demo-d08', 'fotos/demo-08.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'El Tall',
        'Postes sin luz en la carretera hacia el pueblo; de noche no se ve a los peatones.',
        -5.4102, -80.6790, 16.0, 'Recibido', NULL, NULL, 'demo-e29', 'fotos/demo-e29.jpg');
    PERFORM pg_temp.demo_vinculado(e24,
        'Es el mismo bache de la entrada; anoche otro mototaxi casi se voltea.',
        -4.6571, -80.2453, 15.0, 'demo-v03', 'fotos/demo-v03.jpg');
    c12 := pg_temp.demo_caso('Fuga', 'La Arena',
        'Desagüe colapsado frente a las viviendas: aguas servidas corriendo por la calle. Los niños pasan por ahí para ir al colegio.',
        -5.3398, -80.7092, 14.7, 'En proceso', 4, NULL, 'demo-d09', 'fotos/demo-09.jpg');
    PERFORM pg_temp.demo_caso('Fuga', 'Castilla',
        'Fuga en una conexión domiciliaria; el agua corre hacia la pista desde hace varios días.',
        -5.1990, -80.6120, 13.0, 'Atendido', 2, 6, 'demo-e30', 'fotos/demo-e30.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'Veintis',
        'Varios baches seguidos en la avenida; el pavimento se rompió después de las lluvias y los mototaxis se malogran las llantas.',
        -5.1705, -80.6612, 12.2, 'Atendido', 3, 7, 'demo-d11', 'fotos/demo-11.jpg');
    PERFORM pg_temp.demo_vinculado(c12,
        'Es el mismo desagüe. El olor ya es insoportable y hay mosquitos en toda la cuadra.',
        -5.3401, -80.7095, 12.0, 'demo-d10', 'fotos/demo-10.jpg');
    PERFORM pg_temp.demo_vinculado(e20,
        'Sigue igual; el agua sucia llegó hasta la esquina de la plaza.',
        -4.9266, -80.3449, 12.0, 'demo-v02', 'fotos/demo-v02.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'Veintis',
        'Baches en la avenida principal cerca del óvalo; los carros zigzaguean para esquivarlos.',
        -5.1838, -80.6560, 10.0, 'En proceso', 3, NULL, 'demo-e31', 'fotos/demo-e31.jpg');
    c10 := pg_temp.demo_caso('Cables', 'Piura',
        'Poste inclinado con cables colgando a baja altura sobre la vereda, cerca de un paradero. Hay riesgo de accidente si pasa un camión alto.',
        -5.1912, -80.6355, 9.5, 'En proceso', 2, NULL, 'demo-d12', 'fotos/demo-12.jpg');
    PERFORM pg_temp.demo_vinculado(e27,
        'El charco de la parroquia sigue ahí; los fieles entran con los zapatos mojados.',
        -5.1957, -80.6346, 9.0, 'demo-v04', 'fotos/demo-v04.jpg');
    PERFORM pg_temp.demo_vinculado(c10,
        'Confirmo: los cables siguen bajos y el poste cada vez más inclinado.',
        -5.1914, -80.6352, 8.0, 'demo-d13', 'fotos/demo-13.jpg');
    PERFORM pg_temp.demo_caso('Acumulaci', 'Tambo Grande',
        'Basura acumulada en la plaza después de la fiesta patronal.',
        -4.9275, -80.3435, 6.5, 'Recibido', NULL, NULL, 'demo-e32', 'fotos/demo-e32.jpg');
    PERFORM pg_temp.demo_caso('Acumulaci', 'Castilla',
        'El camión recolector no pasa hace tres días y los vecinos sacaron la basura a la esquina. Ya se juntó bastante y hay perros rompiendo las bolsas.',
        -5.1998, -80.6173, 6.3, 'Atendido', 0.5, 3, 'demo-d14', 'fotos/demo-14.jpg');
    PERFORM pg_temp.demo_caso('Sem', 'Veintis',
        'Señales de tránsito despintadas en el cruce; los conductores no respetan el paso peatonal.',
        -5.1822, -80.6675, 5.8, 'Recibido', NULL, NULL, 'demo-e33', 'fotos/demo-e33.jpg');
    PERFORM pg_temp.demo_caso('Bache', 'Catacaos',
        'Hueco grande a la entrada del pueblo, a pocos metros de la plaza de armas. Se malogran las llantas de mototaxis y bicicletas.',
        -5.2664, -80.6741, 5.2, 'Recibido', NULL, NULL, 'demo-d15', 'fotos/demo-15.jpg');
    e34 := pg_temp.demo_caso('Grieta', 'Castilla',
        'Hundimiento en la pista por una fuga subterránea; cada día se ve más profundo.',
        -5.2070, -80.6125, 4.9, 'En proceso', 1, NULL, 'demo-e34', 'fotos/demo-e34.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Veintis',
        'La luminaria del parque principal quedó apagada tras la lluvia y la loza deportiva está a oscuras. Los jóvenes que practican se quedan sin luz.',
        -5.1768, -80.6584, 4.4, 'Atendido', 0.6, 2.5, 'demo-d16', 'fotos/demo-16.jpg');
    c6 := pg_temp.demo_caso('Sem', 'Piura',
        'El semáforo del cruce principal parpadea en ámbar y no cambia a verde. Se forman colas largas y ya hubo varios roces entre carros.',
        -5.1930, -80.6262, 3.6, 'En proceso', 1.5, NULL, 'demo-d17', 'fotos/demo-17.jpg');
    PERFORM pg_temp.demo_caso('Vereda', 'Catacaos',
        'Vereda rota en la salida del colegio; los niños tropiezan al salir corriendo.',
        -5.2690, -80.6755, 3.4, 'Recibido', NULL, NULL, 'demo-e35', 'fotos/demo-e35.jpg');
    PERFORM pg_temp.demo_caso('Vereda', 'Castilla',
        'Vereda levantada por las raíces de un árbol. Una señora se tropezó ayer; falta reparar el tramo frente al colegio.',
        -5.2036, -80.6118, 3.1, 'Recibido', NULL, NULL, 'demo-d18', 'fotos/demo-18.jpg');
    PERFORM pg_temp.demo_caso('_rbol', 'Piura',
        'Ramas secas sobre la vereda del parque; se caen cuando hay viento.',
        -5.1935, -80.6270, 2.7, 'Recibido', NULL, NULL, 'demo-e36', 'fotos/demo-e36.jpg');
    PERFORM pg_temp.demo_vinculado(c6,
        'Es el mismo semáforo. Hoy en la mañana estuvo igual y la cola llegaba hasta la otra cuadra.',
        -5.1931, -80.6264, 2.5, 'demo-d23', 'fotos/demo-23.jpg');
    c4 := pg_temp.demo_caso('Fuga', 'Piura',
        'Fuga de agua potable en la vereda; corre por la pista desde ayer y forma un charco grande que salpica a los que pasan.',
        -5.1983, -80.6335, 2.3, 'En proceso', 1.1, NULL, 'demo-d19', 'fotos/demo-19.jpg');
    PERFORM pg_temp.demo_vinculado(e34,
        'El hundimiento creció; ahora se ve agua saliendo por la grieta.',
        -5.2071, -80.6123, 2.1, 'demo-v05', 'fotos/demo-v05.jpg');
    PERFORM pg_temp.demo_vinculado(c4,
        'Sigue la fuga, ya perdieron bastante agua. Se ve el chorro desde la esquina.',
        -5.1985, -80.6338, 1.9, 'demo-d24', 'fotos/demo-24.jpg');
    c3 := pg_temp.demo_caso('Acumulaci', 'Veintis',
        'Desmonte y bolsas de basura acumuladas en la esquina hace más de una semana. Ya hay mal olor y moscas cerca de las casas.',
        -5.1742, -80.6528, 1.8, 'En proceso', 0.8, NULL, 'demo-d20', 'fotos/demo-20.jpg');
    PERFORM pg_temp.demo_caso('Luminaria', 'Piura',
        'Luminaria que parpadea toda la noche en la esquina y luego se apaga por completo.',
        -5.1995, -80.6355, 1.6, 'En proceso', 0.5, NULL, 'demo-e37', 'fotos/demo-e37.jpg');
    c2 := pg_temp.demo_caso('Luminaria', 'Castilla',
        'Tres postes seguidos sin luz en la cuadra del parque. Zona oscura; los vecinos tienen miedo de salir de noche.',
        -5.2018, -80.6142, 1.2, 'Recibido', NULL, NULL, 'demo-d21', 'fotos/demo-21.jpg');
    PERFORM pg_temp.demo_vinculado(c3,
        'La basura sigue ahí y ahora también hay desmonte. Pasen pronto, por favor.',
        -5.1744, -80.6531, 1.1, 'demo-d25', 'fotos/demo-25.jpg');
    PERFORM pg_temp.demo_caso('Otra', 'Piura',
        'Paradero de bus sin techo y con el letrero tirado en el suelo.',
        -5.1920, -80.6320, 0.9, 'Recibido', NULL, NULL, 'demo-e38', 'fotos/demo-e38.jpg');
    PERFORM pg_temp.demo_vinculado(c2,
        'Sigue sin luz el parque; ya van varios días y se juntan personas sospechosas.',
        -5.2016, -80.6145, 0.8, 'demo-d27', 'fotos/demo-27.jpg');
    PERFORM pg_temp.demo_vinculado(c3,
        'Mismo punto de basura. Los vecinos ya no saben qué hacer con el olor.',
        -5.1740, -80.6526, 0.6, 'demo-d26', 'fotos/demo-26.jpg');
    c1 := pg_temp.demo_caso('Bache', 'Piura',
        'Bache profundo en el carril derecho, cerca del cruce con la avenida principal. Las motos y los carros lo esquivan y es peligroso de noche.',
        -5.1962, -80.6301, 0.4, 'Recibido', NULL, NULL, 'demo-d22', 'fotos/demo-22.jpg');
    PERFORM pg_temp.demo_caso('Fuga', 'La Uni',
        'Agua potable saliendo de la pista frente a la municipalidad del distrito.',
        -5.4010, -80.7410, 0.3, 'Recibido', NULL, NULL, 'demo-e39', 'fotos/demo-e39.jpg');
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
