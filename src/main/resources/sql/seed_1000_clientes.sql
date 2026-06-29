-- ═══════════════════════════════════════════════════════════════════════════════
-- SEED: 1000+ Clientes para llenar Dashboards
-- ═══════════════════════════════════════════════════════════════════════════════
-- Este script inserta:
--   • 1050 clientes únicos
--   • 1050+ cuentas (diferentes tipos)
--   • 2100+ créditos en varios estados (EN_EVALUACION, APROBADO, DESEMBOLSADO, etc.)
--   • Distribución realista de scores y RDS para visualizar semáforos
-- ═══════════════════════════════════════════════════════════════════════════════

-- Obtener IDs de personal del banco (sin perder los existentes)
WITH usuarios_core AS (
  SELECT id, rol FROM usuarios WHERE rol IN ('ASESOR', 'ADMIN', 'JEFE_REGIONAL', 'RIESGOS', 'COMITE')
),

asesor_ids AS (
  SELECT ARRAY_AGG(id) as ids FROM usuarios_core WHERE rol = 'ASESOR'
),

-- ═══════════════════════════════════════════════════════════════════════════════
-- INSERTAR 1050 CLIENTES (evitando duplicados)
-- ═══════════════════════════════════════════════════════════════════════════════
nuevos_clientes AS (
  INSERT INTO usuarios (
    nombre, email, password, dni, rol, activo, created_at
  )
  SELECT
    'Cliente ' || i || ' ' || 
    CASE (i % 5) 
      WHEN 0 THEN 'García'
      WHEN 1 THEN 'López'
      WHEN 2 THEN 'Martínez'
      WHEN 3 THEN 'Rodríguez'
      ELSE 'Fernández'
    END as nombre,
    'cliente' || i || '@banco.pe' as email,
    '$2a$10$N9qo8uLOickgx2ZMRZoMye81YKJ1h9vhW4PULXDqDvvXy7IlNlpmu' as password, -- "password"
    LPAD((1000000 + i)::text, 8, '0') as dni,
    'CLIENTE' as rol,
    true as activo,
    NOW() as created_at
  FROM GENERATE_SERIES(1, 1050) as i
  WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'cliente' || i || '@banco.pe')
  RETURNING id, nombre, email
)

-- ═══════════════════════════════════════════════════════════════════════════════
-- INSERTAR CUENTAS PARA CADA CLIENTE
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO cuentas (numero_cuenta, cci, tipo_cuenta, saldo, moneda, activa, usuario_id, fecha_apertura, created_at)
SELECT
  LPAD((100000000 + nc.id)::text, 16, '0') as numero_cuenta,
  LPAD((100000000 + nc.id)::text, 16, '0') || 'XXX' as cci,
  CASE (nc.id % 3)
    WHEN 0 THEN 'AHORROS'
    WHEN 1 THEN 'CORRIENTE'
    ELSE 'DIGITAL'
  END as tipo_cuenta,
  (RANDOM() * 50000 + 1000)::NUMERIC(15, 2) as saldo,
  'PEN' as moneda,
  true as activa,
  nc.id as usuario_id,
  CURRENT_DATE - (RANDOM() * 365)::INT as fecha_apertura,
  NOW() as created_at
FROM nuevos_clientes nc
WHERE NOT EXISTS (SELECT 1 FROM cuentas WHERE usuario_id = nc.id);

-- ═══════════════════════════════════════════════════════════════════════════════
-- INSERTAR CRÉDITOS EN VARIOS ESTADOS
-- ═══════════════════════════════════════════════════════════════════════════════

-- Tabla auxiliar de asesores (reutilizamos)
WITH asesor_list AS (
  SELECT id, nombre FROM usuarios WHERE rol = 'ASESOR' ORDER BY id LIMIT 7
),

productos AS (
  SELECT 'PERSONAL' as tipo
  UNION SELECT 'HIPOTECARIO'
  UNION SELECT 'VEHICULAR'
  UNION SELECT 'AGROPECUARIO'
  UNION SELECT 'MICROEMPRESA'
),

estados_credito AS (
  SELECT 'EN_EVALUACION' as estado, 1 as peso
  UNION SELECT 'APROBADO', 2
  UNION SELECT 'DESEMBOLSADO', 3
  UNION SELECT 'RECHAZADO', 1
  UNION SELECT 'PENDIENTE_RIESGOS', 1
),

ruta_aprobacion AS (
  SELECT 'ASESOR' as ruta
  UNION SELECT 'ADMIN'
  UNION SELECT 'JEFE_REGIONAL'
),

cliente_cuenta AS (
  SELECT u.id as cliente_id, u.nombre as cliente_nombre, u.email as cliente_email, c.id as cuenta_id, c.numero_cuenta
  FROM usuarios u
  JOIN cuentas c ON c.usuario_id = u.id
  WHERE u.rol = 'CLIENTE'
  LIMIT 1050
)

INSERT INTO creditos (
  numero_operacion, cliente_id, asesor_id, aprobado_por_id,
  tipo_producto, monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual,
  moneda, proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
  rds_ratio, rds_semaforo, es_sujeto_credito, estado, ruta_aprobacion,
  cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota,
  comentario_evaluacion, created_at, updated_at
)
SELECT
  'CRED-' || TO_CHAR(NOW(), 'YYYY') || '-' || LPAD((100000 + ROW_NUMBER() OVER ())::text, 6, '0') as numero_operacion,
  cc.cliente_id,
  al.id as asesor_id,
  CASE WHEN ROW_NUMBER() OVER () % 3 = 0 THEN al.id ELSE NULL END as aprobado_por_id,
  (ARRAY(SELECT tipo FROM productos ORDER BY RANDOM() LIMIT 1))[1] as tipo_producto,
  (RANDOM() * 200000 + 500)::NUMERIC(15, 2) as monto_solicitado,
  CASE WHEN (ARRAY(SELECT estado FROM estados_credito WHERE estado IN ('APROBADO', 'DESEMBOLSADO') ORDER BY RANDOM() LIMIT 1))[1] IS NOT NULL
    THEN (RANDOM() * 200000 + 500)::NUMERIC(15, 2)
    ELSE NULL
  END as monto_aprobado,
  CASE 
    WHEN (ARRAY(SELECT tipo FROM productos ORDER BY RANDOM() LIMIT 1))[1] = 'PERSONAL' THEN 0.3500
    WHEN (ARRAY(SELECT tipo FROM productos ORDER BY RANDOM() LIMIT 1))[1] = 'HIPOTECARIO' THEN 0.0850
    WHEN (ARRAY(SELECT tipo FROM productos ORDER BY RANDOM() LIMIT 1))[1] = 'VEHICULAR' THEN 0.1600
    WHEN (ARRAY(SELECT tipo FROM productos ORDER BY RANDOM() LIMIT 1))[1] = 'AGROPECUARIO' THEN 0.2500
    ELSE 0.4000
  END::NUMERIC(6, 4) as tea,
  (RANDOM() * 50 + 6)::INT as plazo_meses,
  ((RANDOM() * 200000 + 500) * 0.05)::NUMERIC(15, 2) as cuota_mensual,
  'PEN' as moneda,
  CASE WHEN ROW_NUMBER() OVER () % 5 = 0 THEN 'Inversión' 
       WHEN ROW_NUMBER() OVER () % 5 = 1 THEN 'Educación'
       WHEN ROW_NUMBER() OVER () % 5 = 2 THEN 'Negocio'
       WHEN ROW_NUMBER() OVER () % 5 = 3 THEN 'Vivienda'
       ELSE 'Consumo'
  END as proposito,
  (RANDOM() * 1000)::INT as score_crediticio,
  (RANDOM() * 20000 + 2000)::NUMERIC(15, 2) as ingreso_mensual,
  (RANDOM() * 10000)::NUMERIC(15, 2) as deuda_total_vigente,
  (RANDOM() * 0.7)::NUMERIC(6, 4) as rds_ratio,
  CASE 
    WHEN (RANDOM() * 0.7)::NUMERIC(6, 4) < 0.30 THEN 'VERDE'
    WHEN (RANDOM() * 0.7)::NUMERIC(6, 4) < 0.50 THEN 'AMARILLO'
    ELSE 'ROJO'
  END as rds_semaforo,
  RANDOM() > 0.1 as es_sujeto_credito,
  (ARRAY(SELECT estado FROM estados_credito ORDER BY RANDOM() LIMIT 1))[1] as estado,
  (ARRAY(SELECT ruta FROM ruta_aprobacion ORDER BY RANDOM() LIMIT 1))[1] as ruta_aprobacion,
  cc.cuenta_id as cuenta_desembolso_id,
  CASE WHEN (ARRAY(SELECT estado FROM estados_credito WHERE estado = 'DESEMBOLSADO' ORDER BY RANDOM() LIMIT 1))[1] IS NOT NULL
    THEN CURRENT_DATE - (RANDOM() * 180)::INT
    ELSE NULL
  END as fecha_desembolso,
  CASE WHEN (ARRAY(SELECT estado FROM estados_credito WHERE estado = 'DESEMBOLSADO' ORDER BY RANDOM() LIMIT 1))[1] IS NOT NULL
    THEN CURRENT_DATE - (RANDOM() * 180)::INT + INTERVAL '30 days'
    ELSE NULL
  END as fecha_primera_cuota,
  CASE WHEN (ARRAY(SELECT estado FROM estados_credito WHERE estado = 'DESEMBOLSADO' ORDER BY RANDOM() LIMIT 1))[1] IS NOT NULL
    THEN CURRENT_DATE - (RANDOM() * 180)::INT + INTERVAL '180 days'
    ELSE NULL
  END as fecha_ultima_cuota,
  CASE WHEN (RANDOM() * 1000)::INT < 700 THEN 'Evaluación pendiente'
       WHEN (RANDOM() * 1000)::INT < 750 THEN 'Requiere documentación adicional'
       WHEN (RANDOM() * 1000)::INT < 850 THEN 'Aprobado - Excelente perfil'
       ELSE 'Desembolsado'
  END as comentario_evaluacion,
  NOW() as created_at,
  NOW() as updated_at
FROM cliente_cuenta cc
CROSS JOIN asesor_list al
WHERE ROW_NUMBER() OVER (PARTITION BY cc.cliente_id ORDER BY RANDOM()) <= 2  -- 2 créditos por cliente
;

-- ═══════════════════════════════════════════════════════════════════════════════
-- VERIFICACIÓN FINAL
-- ═══════════════════════════════════════════════════════════════════════════════

SELECT 
  'Usuarios CLIENTE' as tabla,
  COUNT(*) as total
FROM usuarios 
WHERE rol = 'CLIENTE'

UNION ALL

SELECT 
  'Cuentas',
  COUNT(*)
FROM cuentas

UNION ALL

SELECT 
  'Créditos',
  COUNT(*)
FROM creditos

UNION ALL

SELECT 
  'Créditos EN_EVALUACION',
  COUNT(*)
FROM creditos
WHERE estado = 'EN_EVALUACION'

UNION ALL

SELECT 
  'Créditos DESEMBOLSADOS',
  COUNT(*)
FROM creditos
WHERE estado = 'DESEMBOLSADO';

