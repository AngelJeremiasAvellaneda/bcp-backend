-- ═══════════════════════════════════════════════════════════════════════════════
-- GENERAR CUOTAS, MOVIMIENTOS Y GESTIONES PARA CRÉDITOS EXISTENTES
-- ═══════════════════════════════════════════════════════════════════════════════

-- PASO 1: GENERAR CUOTAS PARA CRÉDITOS DESEMBOLSADOS
INSERT INTO cuotas_credito (
  credito_id, numero_cuota, fecha_vencimiento, fecha_pago,
  capital, interes, seguro, cuota_total, saldo_capital,
  dias_mora, monto_mora, estado_cuota, created_at
)
SELECT
  c.id,
  n.numero_cuota,
  c.fecha_primera_cuota + ((n.numero_cuota - 1) * INTERVAL '1 month'),
  CASE WHEN (n.numero_cuota % 5 = 0) 
    THEN c.fecha_primera_cuota + ((n.numero_cuota - 1) * INTERVAL '1 month')
    ELSE NULL 
  END,
  (c.monto_aprobado / c.plazo_meses)::NUMERIC(15, 2),
  ((c.monto_aprobado / c.plazo_meses) * (c.tea / 12))::NUMERIC(15, 2),
  CASE WHEN c.tipo_producto = 'HIPOTECARIO' 
    THEN ((c.monto_aprobado / c.plazo_meses) * 0.01)::NUMERIC(15, 2) 
    ELSE 0::NUMERIC(15, 2)
  END,
  ((c.monto_aprobado / c.plazo_meses) * (1 + c.tea / 12))::NUMERIC(15, 2),
  (c.monto_aprobado - (n.numero_cuota * c.monto_aprobado / c.plazo_meses))::NUMERIC(15, 2),
  CASE 
    WHEN (n.numero_cuota % 3 = 0) AND (n.numero_cuota <= 3) 
    THEN (n.numero_cuota * 10)
    ELSE 0 
  END,
  CASE 
    WHEN (n.numero_cuota % 3 = 0) AND (n.numero_cuota <= 3) 
    THEN ((c.monto_aprobado / c.plazo_meses) * (1 + c.tea / 12) * 0.10)::NUMERIC(15, 2)
    ELSE 0::NUMERIC(15, 2)
  END,
  CASE 
    WHEN (n.numero_cuota % 3 = 0) AND (n.numero_cuota <= 3) THEN 'VENCIDA'::TEXT
    WHEN (n.numero_cuota % 5 = 0) THEN 'PAGADA'::TEXT
    ELSE 'PENDIENTE'::TEXT
  END,
  NOW()
FROM creditos c
CROSS JOIN LATERAL GENERATE_SERIES(1, c.plazo_meses) AS n(numero_cuota)
WHERE c.estado = 'DESEMBOLSADO' AND c.monto_aprobado IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM cuotas_credito cc WHERE cc.credito_id = c.id)
ON CONFLICT DO NOTHING;

-- PASO 2: GENERAR MOVIMIENTOS DE DESEMBOLSO
INSERT INTO movimientos (
  cuenta_id, tipo, monto, saldo_anterior, saldo_posterior,
  descripcion, cuenta_destino, created_at
)
SELECT
  c.cuenta_desembolso_id,
  'DEPOSITO'::TEXT,
  c.monto_aprobado,
  (c_cuenta.saldo - c.monto_aprobado)::NUMERIC(15, 2),
  c_cuenta.saldo,
  'Desembolso ' || c.numero_operacion,
  NULL,
  c.fecha_desembolso AT TIME ZONE 'UTC'
FROM creditos c
JOIN cuentas c_cuenta ON c.cuenta_desembolso_id = c_cuenta.id
WHERE c.estado = 'DESEMBOLSADO' AND c.monto_aprobado IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM movimientos m 
    WHERE m.cuenta_id = c.cuenta_desembolso_id 
    AND m.descripcion LIKE 'Desembolso ' || c.numero_operacion
  )
ON CONFLICT DO NOTHING;

-- PASO 3: GENERAR GESTIONES DE COBRANZA PARA CUOTAS VENCIDAS
-- Solo si existen gestores disponibles
INSERT INTO gestiones_cobranza (
  credito_id, cuota_id, gestor_id, tipo_gestion,
  resultado, descripcion, fecha_compromiso_pago, dias_mora_al_gestionar, created_at
)
SELECT
  cc.credito_id,
  cc.id,
  (SELECT id FROM usuarios WHERE rol = 'RIESGOS' ORDER BY RANDOM() LIMIT 1),
  'LLAMADA_TELEFONICA'::TEXT,
  'PROMESA_PAGO'::TEXT,
  'Gestión automática de cobranza - Promesa de pago en 5 días',
  CURRENT_DATE + INTERVAL '5 days',
  cc.dias_mora,
  NOW()
FROM cuotas_credito cc
WHERE cc.estado_cuota = 'VENCIDA'
  AND EXISTS (SELECT 1 FROM usuarios WHERE rol = 'RIESGOS')
  AND NOT EXISTS (
    SELECT 1 FROM gestiones_cobranza gc 
    WHERE gc.cuota_id = cc.id
  )
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- VERIFICACIÓN FINAL
-- ═══════════════════════════════════════════════════════════════════════════════

SELECT 'RESUMEN DE DATOS GENERADOS' as categoria;

SELECT 
  'Total de Créditos' as descripcion,
  COUNT(*) as cantidad
FROM creditos

UNION ALL

SELECT 
  'Total de Cuotas',
  COUNT(*)
FROM cuotas_credito

UNION ALL

SELECT 
  'Total de Movimientos',
  COUNT(*)
FROM movimientos

UNION ALL

SELECT 
  'Total de Gestiones Cobranza',
  COUNT(*)
FROM gestiones_cobranza

UNION ALL

SELECT 
  'Créditos DESEMBOLSADOS (con cuotas)',
  COUNT(*)
FROM creditos
WHERE estado = 'DESEMBOLSADO'

UNION ALL

SELECT 
  'Cuotas VENCIDAS (con mora)',
  COUNT(*)
FROM cuotas_credito
WHERE estado_cuota = 'VENCIDA'

UNION ALL

SELECT 
  'Cuotas PAGADA',
  COUNT(*)
FROM cuotas_credito
WHERE estado_cuota = 'PAGADA';
