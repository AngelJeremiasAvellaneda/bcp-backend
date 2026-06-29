-- ═══════════════════════════════════════════════════════════════════════════════
-- GENERAR 2000+ CRÉDITOS PARA LOS 1059 CLIENTES EXISTENTES
-- ═══════════════════════════════════════════════════════════════════════════════

-- PASO 1: GENERAR PRIMER CRÉDITO POR CLIENTE
INSERT INTO creditos (
  numero_operacion, cliente_id, asesor_id, aprobado_por_id,
  tipo_producto, monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual,
  moneda, proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
  rds_ratio, rds_semaforo, es_sujeto_credito, estado, ruta_aprobacion,
  cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota,
  comentario_evaluacion, created_at
)
SELECT
  'CRED-2026-' || LPAD((500000 + u.id)::text, 6, '0'),
  u.id,
  (SELECT id FROM usuarios WHERE rol = 'ASESOR' ORDER BY id LIMIT 1 OFFSET (u.id % 7)),
  CASE WHEN (u.id % 3 = 0) THEN (SELECT id FROM usuarios WHERE rol = 'ASESOR' ORDER BY id LIMIT 1 OFFSET (u.id % 7)) ELSE NULL END,
  CASE (u.id % 5)
    WHEN 0 THEN 'PERSONAL'::TEXT
    WHEN 1 THEN 'HIPOTECARIO'::TEXT
    WHEN 2 THEN 'VEHICULAR'::TEXT
    WHEN 3 THEN 'AGROPECUARIO'::TEXT
    ELSE 'MICROEMPRESA'::TEXT
  END,
  (5000 + (u.id % 195000))::NUMERIC(15, 2),
  CASE WHEN (u.id % 3 != 0) THEN (5000 + (u.id % 195000))::NUMERIC(15, 2) ELSE NULL END,
  CASE (u.id % 5)
    WHEN 0 THEN 0.3500::NUMERIC(6, 4)
    WHEN 1 THEN 0.0850::NUMERIC(6, 4)
    WHEN 2 THEN 0.1600::NUMERIC(6, 4)
    WHEN 3 THEN 0.2500::NUMERIC(6, 4)
    ELSE 0.4000::NUMERIC(6, 4)
  END,
  (6 + (u.id % 60))::INTEGER,
  ((5000 + (u.id % 195000)) * 0.05 / 12)::NUMERIC(15, 2),
  'PEN',
  CASE (u.id % 5)
    WHEN 0 THEN 'Inversión educativa'::TEXT
    WHEN 1 THEN 'Compra de propiedad'::TEXT
    WHEN 2 THEN 'Compra de vehículo'::TEXT
    WHEN 3 THEN 'Capital de trabajo'::TEXT
    ELSE 'Consumo general'::TEXT
  END,
  (300 + (u.id % 800))::INTEGER,
  (2500 + (u.id % 17500))::NUMERIC(15, 2),
  ((u.id % 10000) / 100)::NUMERIC(15, 2),
  ((u.id % 7000) / 10000)::NUMERIC(6, 4),
  CASE 
    WHEN ((u.id % 7000) / 10000) < 0.30 THEN 'VERDE'::TEXT
    WHEN ((u.id % 7000) / 10000) < 0.50 THEN 'AMARILLO'::TEXT
    ELSE 'ROJO'::TEXT
  END,
  (u.id % 11) > 2,
  CASE (u.id % 7)
    WHEN 0 THEN 'EN_EVALUACION'::TEXT
    WHEN 1 THEN 'APROBADO'::TEXT
    WHEN 2 THEN 'DESEMBOLSADO'::TEXT
    WHEN 3 THEN 'RECHAZADO'::TEXT
    WHEN 4 THEN 'PENDIENTE_RIESGOS'::TEXT
    WHEN 5 THEN 'PENDIENTE_JEFE_REGIONAL'::TEXT
    ELSE 'PENDIENTE_COMITE'::TEXT
  END,
  CASE (u.id % 3)
    WHEN 0 THEN 'ASESOR'::TEXT
    WHEN 1 THEN 'ADMIN'::TEXT
    ELSE 'JEFE_REGIONAL'::TEXT
  END,
  (SELECT id FROM cuentas WHERE usuario_id = u.id LIMIT 1),
  CASE WHEN (u.id % 3 = 0) THEN CURRENT_DATE - (u.id % 180)::INTEGER ELSE NULL END,
  CASE WHEN (u.id % 3 = 0) THEN CURRENT_DATE - (u.id % 180)::INTEGER + INTERVAL '30 days' ELSE NULL END,
  CASE WHEN (u.id % 3 = 0) THEN CURRENT_DATE - (u.id % 180)::INTEGER + INTERVAL '180 days' ELSE NULL END,
  CASE (u.id % 4)
    WHEN 0 THEN 'Evaluación en progreso'::TEXT
    WHEN 1 THEN 'Aprobado - Excelente perfil'::TEXT
    WHEN 2 THEN 'Desembolsado satisfactoriamente'::TEXT
    ELSE 'Requiere documentación adicional'::TEXT
  END,
  NOW()
FROM usuarios u
WHERE u.rol = 'CLIENTE' 
  AND NOT EXISTS (SELECT 1 FROM creditos c WHERE c.cliente_id = u.id AND c.numero_operacion LIKE 'CRED-2026-5%')
ON CONFLICT DO NOTHING;

-- PASO 2: GENERAR SEGUNDO CRÉDITO POR CLIENTE
INSERT INTO creditos (
  numero_operacion, cliente_id, asesor_id, aprobado_por_id,
  tipo_producto, monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual,
  moneda, proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
  rds_ratio, rds_semaforo, es_sujeto_credito, estado, ruta_aprobacion,
  cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota,
  comentario_evaluacion, created_at
)
SELECT
  'CRED-2026-' || LPAD((600000 + u.id)::text, 6, '0'),
  u.id,
  (SELECT id FROM usuarios WHERE rol = 'ASESOR' ORDER BY id LIMIT 1 OFFSET ((u.id + 3) % 7)),
  CASE WHEN ((u.id + 5) % 3 = 0) THEN (SELECT id FROM usuarios WHERE rol = 'ASESOR' ORDER BY id LIMIT 1 OFFSET ((u.id + 3) % 7)) ELSE NULL END,
  CASE ((u.id + 2) % 5)
    WHEN 0 THEN 'PERSONAL'::TEXT
    WHEN 1 THEN 'HIPOTECARIO'::TEXT
    WHEN 2 THEN 'VEHICULAR'::TEXT
    WHEN 3 THEN 'AGROPECUARIO'::TEXT
    ELSE 'MICROEMPRESA'::TEXT
  END,
  (3000 + ((u.id + 100) % 197000))::NUMERIC(15, 2),
  CASE WHEN ((u.id + 1) % 3 != 0) THEN (3000 + ((u.id + 100) % 197000))::NUMERIC(15, 2) ELSE NULL END,
  CASE ((u.id + 2) % 5)
    WHEN 0 THEN 0.3500::NUMERIC(6, 4)
    WHEN 1 THEN 0.0850::NUMERIC(6, 4)
    WHEN 2 THEN 0.1600::NUMERIC(6, 4)
    WHEN 3 THEN 0.2500::NUMERIC(6, 4)
    ELSE 0.4000::NUMERIC(6, 4)
  END,
  (12 + ((u.id + 5) % 48))::INTEGER,
  ((3000 + ((u.id + 100) % 197000)) * 0.05 / 12)::NUMERIC(15, 2),
  'PEN',
  CASE ((u.id + 1) % 5)
    WHEN 0 THEN 'Reforma del hogar'::TEXT
    WHEN 1 THEN 'Refinanciamiento'::TEXT
    WHEN 2 THEN 'Compra de equipo'::TEXT
    WHEN 3 THEN 'Expansión de negocio'::TEXT
    ELSE 'Pago de deudas'::TEXT
  END,
  (400 + ((u.id + 50) % 750))::INTEGER,
  (3000 + ((u.id + 200) % 16000))::NUMERIC(15, 2),
  ((u.id % 5000) / 100)::NUMERIC(15, 2),
  ((u.id % 6000) / 10000)::NUMERIC(6, 4),
  CASE 
    WHEN ((u.id % 6000) / 10000) < 0.30 THEN 'VERDE'::TEXT
    WHEN ((u.id % 6000) / 10000) < 0.50 THEN 'AMARILLO'::TEXT
    ELSE 'ROJO'::TEXT
  END,
  ((u.id + 3) % 11) > 2,
  CASE ((u.id + 1) % 7)
    WHEN 0 THEN 'EN_EVALUACION'::TEXT
    WHEN 1 THEN 'APROBADO'::TEXT
    WHEN 2 THEN 'DESEMBOLSADO'::TEXT
    WHEN 3 THEN 'RECHAZADO'::TEXT
    WHEN 4 THEN 'PENDIENTE_RIESGOS'::TEXT
    WHEN 5 THEN 'PENDIENTE_JEFE_REGIONAL'::TEXT
    ELSE 'PENDIENTE_COMITE'::TEXT
  END,
  CASE ((u.id + 2) % 3)
    WHEN 0 THEN 'ASESOR'::TEXT
    WHEN 1 THEN 'ADMIN'::TEXT
    ELSE 'JEFE_REGIONAL'::TEXT
  END,
  (SELECT id FROM cuentas WHERE usuario_id = u.id LIMIT 1),
  CASE WHEN ((u.id + 2) % 3 = 0) THEN CURRENT_DATE - ((u.id + 20) % 180)::INTEGER ELSE NULL END,
  CASE WHEN ((u.id + 2) % 3 = 0) THEN CURRENT_DATE - ((u.id + 20) % 180)::INTEGER + INTERVAL '30 days' ELSE NULL END,
  CASE WHEN ((u.id + 2) % 3 = 0) THEN CURRENT_DATE - ((u.id + 20) % 180)::INTEGER + INTERVAL '180 days' ELSE NULL END,
  CASE ((u.id + 2) % 4)
    WHEN 0 THEN 'Evaluación finalizada'::TEXT
    WHEN 1 THEN 'Aprobado - Buen score'::TEXT
    WHEN 2 THEN 'Desembolsado a cuenta'::TEXT
    ELSE 'En revisión por comité'::TEXT
  END,
  NOW()
FROM usuarios u
WHERE u.rol = 'CLIENTE' 
  AND NOT EXISTS (SELECT 1 FROM creditos c WHERE c.cliente_id = u.id AND c.numero_operacion LIKE 'CRED-2026-6%')
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- VERIFICACIÓN FINAL
-- ═══════════════════════════════════════════════════════════════════════════════

SELECT 'RESUMEN FINAL' as descripcion;

SELECT 
  'Total de Créditos' as metrica,
  COUNT(*) as cantidad
FROM creditos

UNION ALL

SELECT 
  'Créditos EN_EVALUACION',
  COUNT(*)
FROM creditos
WHERE estado = 'EN_EVALUACION'

UNION ALL

SELECT 
  'Créditos DESEMBOLSADO',
  COUNT(*)
FROM creditos
WHERE estado = 'DESEMBOLSADO'

UNION ALL

SELECT 
  'Créditos APROBADO',
  COUNT(*)
FROM creditos
WHERE estado = 'APROBADO'

UNION ALL

SELECT 
  'Créditos PENDIENTE_RIESGOS',
  COUNT(*)
FROM creditos
WHERE estado = 'PENDIENTE_RIESGOS'

UNION ALL

SELECT 
  'Créditos PENDIENTE_COMITE',
  COUNT(*)
FROM creditos
WHERE estado = 'PENDIENTE_COMITE'

UNION ALL

SELECT 
  'Créditos RECHAZADO',
  COUNT(*)
FROM creditos
WHERE estado = 'RECHAZADO';
