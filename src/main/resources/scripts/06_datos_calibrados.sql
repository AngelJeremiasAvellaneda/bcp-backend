-- ============================================================
-- Script 06: Datos calibrados — BancoBCP
-- Rúbrica 20/20:
--   Criterio 1: Flujo completo Core ↔ Homebanking
--   Criterio 2: Reglas negocio (scoring, RDS, semáforo, ruta)
--   Criterio 3: RBAC — todos los roles
--   Criterio 4: Cartera morosa — todas las bandas R1/R2/R3
--   Criterio 5: Tasa mora ~13%, 2 productos, integridad referencial
--
-- Password universal: 123456
-- BCrypt hash ($2a$10$...): generado con BCrypt rounds=10
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. USUARIOS
-- ────────────────────────────────────────────────────────────
-- password: 123456  (BCrypt 10 rounds)
INSERT INTO usuarios (nombre, email, password, rol, activo) VALUES
-- Clientes Homebanking
('Ana García López',          'demo@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Pedro Sánchez Ruiz',        'pedro@banco.pe',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Lucía Ramírez Torres',      'lucia@banco.pe',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Miguel Quispe Huanca',      'miguel@banco.pe',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Carmen Flores Vidal',       'carmen@banco.pe',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('José Mamani Ccopa',         'jose@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Rosa Condori Apaza',        'rosa@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Luis Vargas Medina',        'luis@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
-- Personal Core
('Carlos Mendoza Vega',       'asesor@banco.pe',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ASESOR',        true),
('Sofía Paredes Luna',        'asesor2@banco.pe',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ASESOR',        true),
('María Torres Silva',        'admin@banco.pe',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN',         true),
('Roberto Castillo Díaz',     'jefe@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'JEFE_REGIONAL', true),
('Laura Fernández Ortiz',     'riesgos@banco.pe',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'RIESGOS',       true),
('Miguel Ángel Paredes',      'comite@banco.pe',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'COMITE',        true),
('Dr. Jorge Villanueva Reyes','gerencia@banco.pe',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'GERENCIA',      true);

-- ────────────────────────────────────────────────────────────
-- 2. CUENTAS
-- ────────────────────────────────────────────────────────────
-- Usamos subqueries para mapear emails a IDs (independiente del orden de inserción)
INSERT INTO cuentas (numero_cuenta, tipo, saldo, moneda, activa, usuario_id) VALUES
('0011223344550001', 'AHORROS',   4850.00, 'PEN', true, (SELECT id FROM usuarios WHERE email='demo@banco.pe')),
('0011223344550002', 'CORRIENTE', 1200.50, 'PEN', true, (SELECT id FROM usuarios WHERE email='demo@banco.pe')),
('0022334455660001', 'AHORROS',    750.00, 'PEN', true, (SELECT id FROM usuarios WHERE email='pedro@banco.pe')),
('0033445566770001', 'AHORROS',   3200.00, 'PEN', true, (SELECT id FROM usuarios WHERE email='lucia@banco.pe')),
('0044556677880001', 'AHORROS',    500.00, 'PEN', true, (SELECT id FROM usuarios WHERE email='miguel@banco.pe')),
('0055667788990001', 'AHORROS',   1800.00, 'PEN', true, (SELECT id FROM usuarios WHERE email='carmen@banco.pe')),
('0066778899000001', 'AHORROS',    200.00, 'PEN', true, (SELECT id FROM usuarios WHERE email='jose@banco.pe')),
('0077889900110001', 'AHORROS',      0.00, 'PEN', true, (SELECT id FROM usuarios WHERE email='rosa@banco.pe')),
('0088990011220001', 'AHORROS',    950.00, 'PEN', true, (SELECT id FROM usuarios WHERE email='luis@banco.pe'));

-- ────────────────────────────────────────────────────────────
-- 3. MOVIMIENTOS INICIALES (Homebanking)
-- ────────────────────────────────────────────────────────────
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
((SELECT id FROM cuentas WHERE numero_cuenta='0011223344550001'), 'DEPOSITO',               2000.00, 2850.00, 4850.00, 'Depósito inicial apertura'),
((SELECT id FROM cuentas WHERE numero_cuenta='0011223344550001'), 'PAGO_SERVICIO',            185.40, 4850.00, 4664.60, 'Luz del Sur — Mayo 2026'),
((SELECT id FROM cuentas WHERE numero_cuenta='0011223344550001'), 'PAGO_SERVICIO',             89.90, 4664.60, 4574.70, 'Claro Internet — Mayo 2026'),
((SELECT id FROM cuentas WHERE numero_cuenta='0011223344550001'), 'TRANSFERENCIA_ENVIADA',    500.00, 4574.70, 4074.70, 'Envío a Pedro Sánchez'),
((SELECT id FROM cuentas WHERE numero_cuenta='0022334455660001'), 'TRANSFERENCIA_RECIBIDA',   500.00,  250.00,  750.00, 'Recibido de Ana García'),
((SELECT id FROM cuentas WHERE numero_cuenta='0011223344550002'), 'DEPOSITO',               1200.50,    0.00, 1200.50, 'Ingreso sueldo Mayo 2026'),
((SELECT id FROM cuentas WHERE numero_cuenta='0033445566770001'), 'DEPOSITO',               3200.00,    0.00, 3200.00, 'Depósito inicial'),
((SELECT id FROM cuentas WHERE numero_cuenta='0055667788990001'), 'DEPOSITO',               1800.00,    0.00, 1800.00, 'Depósito salario');

-- ────────────────────────────────────────────────────────────
-- 4. CRÉDITOS — FLUJO CORE ↔ HOMEBANKING (Criterio 1 + 2)
-- ────────────────────────────────────────────────────────────

-- C1: PERSONAL S/5,000 — DESEMBOLSADO — al día
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion,
    cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota
) VALUES (
    'CRED-2026-000001',
    (SELECT id FROM usuarios WHERE email='demo@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'PERSONAL', 5000.00, 5000.00, 0.3500, 24, 283.75, 'PEN',
    'Capital de trabajo personal', 750, 2500.00, 283.75, 0.1135, 'VERDE', true,
    'DESEMBOLSADO',
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'Aprobado automáticamente — Score 750, RDS 11%', 'ASESOR',
    (SELECT id FROM cuentas WHERE numero_cuenta='0011223344550001'),
    CURRENT_DATE - INTERVAL '3 months',
    CURRENT_DATE - INTERVAL '2 months',
    CURRENT_DATE + INTERVAL '22 months'
);

-- Desembolso acreditado en cuenta de Ana
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
((SELECT id FROM cuentas WHERE numero_cuenta='0011223344550001'), 'DEPOSITO',
 5000.00, 4850.00, 9850.00, 'Desembolso crédito CRED-2026-000001');

-- Cuotas C1: 2 pagadas + 1 pendiente
INSERT INTO cuotas_credito (credito_id, numero_cuota, fecha_vencimiento, fecha_pago, capital, interes, seguro, cuota_total, saldo_capital, dias_mora, monto_mora, estado_cuota) VALUES
((SELECT id FROM creditos WHERE numero_operacion='CRED-2026-000001'), 1, CURRENT_DATE-INTERVAL '2 months', CURRENT_DATE-INTERVAL '2 months', 208.33, 75.42, 0, 283.75, 4791.67, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2026-000001'), 2, CURRENT_DATE-INTERVAL '1 month',  CURRENT_DATE-INTERVAL '1 month',  211.30, 72.45, 0, 283.75, 4580.37, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2026-000001'), 3, CURRENT_DATE+INTERVAL '10 days',  NULL,                             214.31, 69.44, 0, 283.75, 4366.06, 0, 0.00, 'PENDIENTE');

-- C2: HIPOTECARIO S/80,000 — PENDIENTE_JEFE_REGIONAL
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, comentario_evaluacion, ruta_aprobacion, cuenta_desembolso_id
) VALUES (
    'CRED-2026-000002',
    (SELECT id FROM usuarios WHERE email='demo@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'HIPOTECARIO', 80000.00, 0.0850, 240, 696.04, 'PEN',
    'Adquisición de vivienda — Conjunto Los Pinos, Miraflores',
    820, 4500.00, 283.75, 0.2177, 'VERDE', true,
    'PENDIENTE_JEFE_REGIONAL',
    'Evaluado por asesor. Score 820. RDS proyectado 21.8% — VERDE. Pasa a revisión Jefe Regional.',
    'JEFE_REGIONAL',
    (SELECT id FROM cuentas WHERE numero_cuenta='0011223344550001')
);

-- C3: VEHICULAR S/15,000 — APROBADO (pendiente desembolso)
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion, cuenta_desembolso_id
) VALUES (
    'CRED-2026-000003',
    (SELECT id FROM usuarios WHERE email='lucia@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor2@banco.pe'),
    'VEHICULAR', 15000.00, 15000.00, 0.1600, 48, 425.40, 'PEN',
    'Adquisición vehículo familiar — Toyota Yaris 2025',
    680, 3000.00, 425.40, 0.1418, 'VERDE', true,
    'APROBADO',
    (SELECT id FROM usuarios WHERE email='admin@banco.pe'),
    'Aprobado por Administración. Score 680, RDS 14.2% VERDE.', 'ADMIN',
    (SELECT id FROM cuentas WHERE numero_cuenta='0033445566770001')
);

-- C4: EN_EVALUACION por asesor
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, comentario_evaluacion, ruta_aprobacion, cuenta_desembolso_id
) VALUES (
    'CRED-2026-000004',
    (SELECT id FROM usuarios WHERE email='miguel@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'PERSONAL', 3500.00, 0.3500, 18, 245.00, 'PEN',
    'Capital de trabajo — emprendimiento gastronómico',
    610, 1800.00, 245.00, 0.1361, 'VERDE', true,
    'EN_EVALUACION',
    'En evaluación — pendiente verificación documentaria.', 'ASESOR',
    (SELECT id FROM cuentas WHERE numero_cuenta='0044556677880001')
);

-- C5: RECHAZADO — RDS ROJO + score bajo (demuestra regla de negocio)
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion, cuenta_desembolso_id
) VALUES (
    'CRED-2026-000005',
    (SELECT id FROM usuarios WHERE email='jose@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'PERSONAL', 8000.00, 0.3500, 12, 843.00, 'PEN',
    'Negocio informal',
    350, 1200.00, 843.00, 0.7025, 'ROJO', false,
    'RECHAZADO',
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'RECHAZADO: Score 350/1000 insuficiente (mín. 400). RDS 70.2% — ROJO (máx. 50%).', 'ASESOR',
    (SELECT id FROM cuentas WHERE numero_cuenta='0066778899000001')
);

-- C6: PENDIENTE_RIESGOS (monto alto, RDS AMARILLO)
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, comentario_evaluacion, ruta_aprobacion, cuenta_desembolso_id
) VALUES (
    'CRED-2026-000006',
    (SELECT id FROM usuarios WHERE email='carmen@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor2@banco.pe'),
    'MICROEMPRESA', 45000.00, 0.4000, 36, 1671.00, 'PEN',
    'Expansión negocio textil — compra de maquinaria',
    730, 5500.00, 1671.00, 0.3038, 'AMARILLO', true,
    'PENDIENTE_RIESGOS',
    'Score 730. RDS 30.4% AMARILLO. Monto requiere dictamen de Riesgos.', 'RIESGOS',
    (SELECT id FROM cuentas WHERE numero_cuenta='0055667788990001')
);

-- C7: PENDIENTE_COMITE (monto > S/150,000)
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, comentario_evaluacion, ruta_aprobacion, cuenta_desembolso_id
) VALUES (
    'CRED-2026-000007',
    (SELECT id FROM usuarios WHERE email='luis@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'AGROPECUARIO', 200000.00, 0.2500, 60, 5892.00, 'PEN',
    'Proyecto agroindustrial — parcelas de cultivo quinua',
    790, 12000.00, 5892.00, 0.4910, 'AMARILLO', true,
    'PENDIENTE_COMITE',
    'Score 790. RDS 49.1% AMARILLO. Requiere resolución de Comité por monto > S/150,000.', 'COMITE',
    (SELECT id FROM cuentas WHERE numero_cuenta='0088990011220001')
);

-- ────────────────────────────────────────────────────────────
-- 5. CARTERA MOROSA — TODAS LAS BANDAS (Criterio 4 — R1)
-- ────────────────────────────────────────────────────────────

-- PREVENTIVA: 18 días mora (Pedro)
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion,
    cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota
) VALUES (
    'CRED-2025-000010',
    (SELECT id FROM usuarios WHERE email='pedro@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'PERSONAL', 4000.00, 4000.00, 0.3500, 24, 226.67, 'PEN',
    'Remodelación hogar', 620, 2000.00, 226.67, 0.1133, 'VERDE', true,
    'DESEMBOLSADO',
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'Aprobado — Score 620.', 'ASESOR',
    (SELECT id FROM cuentas WHERE numero_cuenta='0022334455660001'),
    CURRENT_DATE-INTERVAL '5 months',
    CURRENT_DATE-INTERVAL '4 months',
    CURRENT_DATE+INTERVAL '20 months'
);
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
((SELECT id FROM cuentas WHERE numero_cuenta='0022334455660001'), 'DEPOSITO', 4000.00, 750.00, 4750.00, 'Desembolso CRED-2025-000010');
INSERT INTO cuotas_credito (credito_id, numero_cuota, fecha_vencimiento, fecha_pago, capital, interes, seguro, cuota_total, saldo_capital, dias_mora, monto_mora, estado_cuota) VALUES
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000010'), 1, CURRENT_DATE-INTERVAL '4 months', CURRENT_DATE-INTERVAL '4 months', 186.67, 40.00, 0, 226.67, 3813.33, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000010'), 2, CURRENT_DATE-INTERVAL '3 months', CURRENT_DATE-INTERVAL '3 months', 189.43, 37.24, 0, 226.67, 3623.90, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000010'), 3, CURRENT_DATE-INTERVAL '2 months', CURRENT_DATE-INTERVAL '2 months', 192.23, 34.44, 0, 226.67, 3431.67, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000010'), 4, CURRENT_DATE-INTERVAL '18 days',  NULL,                             195.08, 31.59, 0, 226.67, 3236.59, 18, 3.51, 'VENCIDA');

-- TEMPRANA: 45 días mora (Miguel)
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion,
    cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota
) VALUES (
    'CRED-2025-000020',
    (SELECT id FROM usuarios WHERE email='miguel@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor2@banco.pe'),
    'PERSONAL', 3000.00, 3000.00, 0.3500, 12, 289.68, 'PEN',
    'Gastos médicos familiares', 580, 1500.00, 289.68, 0.1931, 'VERDE', true,
    'DESEMBOLSADO',
    (SELECT id FROM usuarios WHERE email='asesor2@banco.pe'),
    'Aprobado — Score 580.', 'ASESOR',
    (SELECT id FROM cuentas WHERE numero_cuenta='0044556677880001'),
    CURRENT_DATE-INTERVAL '6 months',
    CURRENT_DATE-INTERVAL '5 months',
    CURRENT_DATE+INTERVAL '7 months'
);
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
((SELECT id FROM cuentas WHERE numero_cuenta='0044556677880001'), 'DEPOSITO', 3000.00, 500.00, 3500.00, 'Desembolso CRED-2025-000020');
INSERT INTO cuotas_credito (credito_id, numero_cuota, fecha_vencimiento, fecha_pago, capital, interes, seguro, cuota_total, saldo_capital, dias_mora, monto_mora, estado_cuota) VALUES
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000020'), 1, CURRENT_DATE-INTERVAL '5 months', CURRENT_DATE-INTERVAL '5 months', 239.68, 50.00, 0, 289.68, 2760.32, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000020'), 2, CURRENT_DATE-INTERVAL '4 months', CURRENT_DATE-INTERVAL '4 months', 243.69, 45.99, 0, 289.68, 2516.63, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000020'), 3, CURRENT_DATE-INTERVAL '3 months', CURRENT_DATE-INTERVAL '3 months', 247.76, 41.92, 0, 289.68, 2268.87, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000020'), 4, CURRENT_DATE-INTERVAL '45 days',  NULL,                             251.88, 37.80, 0, 289.68, 2017.00, 45, 11.33, 'VENCIDA');

-- TARDÍA: 90 días mora (Carmen)
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion,
    cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota
) VALUES (
    'CRED-2025-000030',
    (SELECT id FROM usuarios WHERE email='carmen@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'VEHICULAR', 12000.00, 12000.00, 0.1600, 36, 421.80, 'PEN',
    'Adquisición moto taxi para trabajo', 600, 2800.00, 421.80, 0.1506, 'VERDE', true,
    'DESEMBOLSADO',
    (SELECT id FROM usuarios WHERE email='admin@banco.pe'),
    'Aprobado por Admin — Score 600.', 'ADMIN',
    (SELECT id FROM cuentas WHERE numero_cuenta='0055667788990001'),
    CURRENT_DATE-INTERVAL '8 months',
    CURRENT_DATE-INTERVAL '7 months',
    CURRENT_DATE+INTERVAL '29 months'
);
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
((SELECT id FROM cuentas WHERE numero_cuenta='0055667788990001'), 'DEPOSITO', 12000.00, 1800.00, 13800.00, 'Desembolso CRED-2025-000030');
INSERT INTO cuotas_credito (credito_id, numero_cuota, fecha_vencimiento, fecha_pago, capital, interes, seguro, cuota_total, saldo_capital, dias_mora, monto_mora, estado_cuota) VALUES
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000030'), 1, CURRENT_DATE-INTERVAL '7 months', CURRENT_DATE-INTERVAL '7 months', 285.80, 136.00, 0, 421.80, 11714.20, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000030'), 2, CURRENT_DATE-INTERVAL '6 months', CURRENT_DATE-INTERVAL '6 months', 289.04, 132.76, 0, 421.80, 11425.16, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000030'), 3, CURRENT_DATE-INTERVAL '5 months', CURRENT_DATE-INTERVAL '5 months', 292.31, 129.49, 0, 421.80, 11132.85, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000030'), 4, CURRENT_DATE-INTERVAL '4 months', CURRENT_DATE-INTERVAL '4 months', 295.63, 126.17, 0, 421.80, 10837.22, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000030'), 5, CURRENT_DATE-INTERVAL '90 days',  NULL,                             298.98, 122.82, 0, 421.80, 10538.24, 90, 26.91, 'VENCIDA');

-- JUDICIAL: 135 días mora (Rosa) — derivado a judicial por Jefe Regional
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion,
    cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota
) VALUES (
    'CRED-2024-000050',
    (SELECT id FROM usuarios WHERE email='rosa@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'PERSONAL', 6000.00, 6000.00, 0.3500, 18, 413.80, 'PEN',
    'Gastos escolares hijos', 520, 1800.00, 413.80, 0.2299, 'VERDE', true,
    'DESEMBOLSADO',
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'Aprobado — Score 520. DERIVADO A JUDICIAL por Roberto Castillo Díaz — 135 días mora.', 'ASESOR',
    (SELECT id FROM cuentas WHERE numero_cuenta='0077889900110001'),
    CURRENT_DATE-INTERVAL '12 months',
    CURRENT_DATE-INTERVAL '11 months',
    CURRENT_DATE+INTERVAL '7 months'
);
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
((SELECT id FROM cuentas WHERE numero_cuenta='0077889900110001'), 'DEPOSITO', 6000.00, 0.00, 6000.00, 'Desembolso CRED-2024-000050');
INSERT INTO cuotas_credito (credito_id, numero_cuota, fecha_vencimiento, fecha_pago, capital, interes, seguro, cuota_total, saldo_capital, dias_mora, monto_mora, estado_cuota) VALUES
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), 1, CURRENT_DATE-INTERVAL '11 months', CURRENT_DATE-INTERVAL '11 months', 310.47, 103.33, 0, 413.80, 5689.53, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), 2, CURRENT_DATE-INTERVAL '10 months', CURRENT_DATE-INTERVAL '10 months', 315.82, 97.98,  0, 413.80, 5373.71, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), 3, CURRENT_DATE-INTERVAL '9 months',  CURRENT_DATE-INTERVAL '9 months',  321.26, 92.54,  0, 413.80, 5052.45, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), 4, CURRENT_DATE-INTERVAL '8 months',  CURRENT_DATE-INTERVAL '8 months',  326.79, 87.01,  0, 413.80, 4725.66, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), 5, CURRENT_DATE-INTERVAL '7 months',  CURRENT_DATE-INTERVAL '7 months',  332.41, 81.39,  0, 413.80, 4393.25, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), 6, CURRENT_DATE-INTERVAL '6 months',  CURRENT_DATE-INTERVAL '6 months',  338.12, 75.68,  0, 413.80, 4055.13, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), 7, CURRENT_DATE-INTERVAL '5 months',  CURRENT_DATE-INTERVAL '5 months',  343.93, 69.87,  0, 413.80, 3711.20, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), 8, CURRENT_DATE-INTERVAL '135 days',  NULL,                              349.84, 63.96,  0, 413.80, 3361.36, 135, 47.23, 'VENCIDA');

-- CASTIGO: 200 días mora (Luis) — castigado contablemente
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion,
    cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota
) VALUES (
    'CRED-2024-000001',
    (SELECT id FROM usuarios WHERE email='luis@banco.pe'),
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'PERSONAL', 2500.00, 2500.00, 0.3500, 12, 241.34, 'PEN',
    'Gastos personales emergencia', 440, 1400.00, 241.34, 0.1724, 'VERDE', true,
    'RECHAZADO',  -- RECHAZADO = estado de crédito castigado contablemente
    (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),
    'Aprobado Score 440. DERIVADO A JUDICIAL — 135 días. CASTIGADO por Dr. Jorge Villanueva Reyes — 200 días mora.', 'ASESOR',
    (SELECT id FROM cuentas WHERE numero_cuenta='0088990011220001'),
    CURRENT_DATE-INTERVAL '14 months',
    CURRENT_DATE-INTERVAL '13 months',
    CURRENT_DATE-INTERVAL '1 month'
);
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
((SELECT id FROM cuentas WHERE numero_cuenta='0088990011220001'), 'DEPOSITO', 2500.00, 950.00, 3450.00, 'Desembolso CRED-2024-000001');
INSERT INTO cuotas_credito (credito_id, numero_cuota, fecha_vencimiento, fecha_pago, capital, interes, seguro, cuota_total, saldo_capital, dias_mora, monto_mora, estado_cuota) VALUES
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000001'), 1, CURRENT_DATE-INTERVAL '13 months', CURRENT_DATE-INTERVAL '13 months', 199.34, 42.00, 0, 241.34, 2300.66, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000001'), 2, CURRENT_DATE-INTERVAL '12 months', CURRENT_DATE-INTERVAL '12 months', 202.68, 38.66, 0, 241.34, 2097.98, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000001'), 3, CURRENT_DATE-INTERVAL '11 months', CURRENT_DATE-INTERVAL '11 months', 206.07, 35.27, 0, 241.34, 1891.91, 0, 0.00, 'PAGADA'),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000001'), 4, CURRENT_DATE-INTERVAL '200 days',  NULL,                              209.51, 31.83, 0, 241.34, 1682.40, 200, 41.90, 'VENCIDA');

-- ────────────────────────────────────────────────────────────
-- 6. GESTIONES DE COBRANZA R2
-- ────────────────────────────────────────────────────────────
INSERT INTO gestiones_cobranza (credito_id, gestor_id, tipo_gestion, resultado, descripcion, fecha_compromiso_pago, dias_mora_al_gestionar) VALUES
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000010'), (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),   'LLAMADA_TELEFONICA', 'PROMESA_PAGO',     'Cliente comprometió pago antes del fin de semana. Mora: 18 días.',    CURRENT_DATE+INTERVAL '5 days', 18),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000020'), (SELECT id FROM usuarios WHERE email='asesor2@banco.pe'),  'SMS',                'CONTACTO_EXITOSO', 'SMS enviado. Cliente informado de deuda y mora acumulada.',           NULL, 45),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000020'), (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),   'LLAMADA_TELEFONICA', 'SIN_CONTACTO',     'No contesta. Intentar visita domiciliaria.',                          NULL, 45),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000030'), (SELECT id FROM usuarios WHERE email='jefe@banco.pe'),     'VISITA_DOMICILIARIA','NEGATIVA_PAGO',    'Cliente negó el pago. Problemas laborales. Se notifica siguiente etapa.', NULL, 90),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2025-000030'), (SELECT id FROM usuarios WHERE email='asesor@banco.pe'),   'CARTA_NOTARIAL',     'OTRO',             'Carta notarial enviada. Plazo 10 días hábiles para regularizar.',      NULL, 90),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), (SELECT id FROM usuarios WHERE email='jefe@banco.pe'),     'CARTA_NOTARIAL',     'OTRO',             'Carta de inicio de proceso judicial enviada al domicilio del cliente.', NULL, 135),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'), (SELECT id FROM usuarios WHERE email='riesgos@banco.pe'),  'OTRO',               'OTRO',             'Expediente enviado a área legal. Bien hipotecado identificado para embargo.', NULL, 135),
((SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000001'), (SELECT id FROM usuarios WHERE email='gerencia@banco.pe'), 'OTRO',               'OTRO',             'Crédito castigado contablemente según resolución de Gerencia. Incobrabilidad declarada.', NULL, 200);

-- ────────────────────────────────────────────────────────────
-- 7. AUDITORÍA (trazabilidad completa — Criterio 3 + 5)
-- ────────────────────────────────────────────────────────────
INSERT INTO auditoria_eventos (email_actor, rol_actor, accion, modulo, descripcion, recurso_id, ip_origen) VALUES
('demo@banco.pe',      'CLIENTE',       'LOGIN',             'AUTH',          'Login exitoso — Homebanking',                                                NULL,                                                                         '127.0.0.1'),
('asesor@banco.pe',    'ASESOR',        'LOGIN',             'AUTH',          'Login exitoso — Core Bancario',                                              NULL,                                                                         '127.0.0.1'),
('demo@banco.pe',      'CLIENTE',       'CREDITO_SOLICITUD', 'CREDITO',       'Solicitud CRED-2026-000001 S/5,000 PERSONAL',                               (SELECT id FROM creditos WHERE numero_operacion='CRED-2026-000001'),           '127.0.0.1'),
('asesor@banco.pe',    'ASESOR',        'CREDITO_APROBACION','CREDITO',       'Aprobado CRED-2026-000001 — Score 750 RDS 11%',                             (SELECT id FROM creditos WHERE numero_operacion='CRED-2026-000001'),           '127.0.0.1'),
('asesor@banco.pe',    'ASESOR',        'CREDITO_DESEMBOLSO','CREDITO',       'Desembolso CRED-2026-000001 S/5,000 → cta 0011223344550001',               (SELECT id FROM creditos WHERE numero_operacion='CRED-2026-000001'),           '127.0.0.1'),
('demo@banco.pe',      'CLIENTE',       'CREDITO_SOLICITUD', 'CREDITO',       'Solicitud CRED-2026-000002 S/80,000 HIPOTECARIO',                           (SELECT id FROM creditos WHERE numero_operacion='CRED-2026-000002'),           '127.0.0.1'),
('jefe@banco.pe',      'JEFE_REGIONAL', 'LOGIN',             'AUTH',          'Login exitoso — Core Bancario',                                              NULL,                                                                         '127.0.0.1'),
('demo@banco.pe',      'CLIENTE',       'CUENTA_TRANSFERENCIA','CUENTA',      'Transferencia S/500 → Pedro Sánchez (0022334455660001)',                    NULL,                                                                         '127.0.0.1'),
('riesgos@banco.pe',   'RIESGOS',       'LOGIN',             'AUTH',          'Login exitoso — Core Bancario',                                              NULL,                                                                         '127.0.0.1'),
('admin@banco.pe',     'ADMIN',         'CREDITO_APROBACION','CREDITO',       'Aprobado CRED-2026-000003 S/15,000 VEHICULAR — Admin',                     (SELECT id FROM creditos WHERE numero_operacion='CRED-2026-000003'),           '127.0.0.1'),
('jefe@banco.pe',      'JEFE_REGIONAL', 'COBRANZA_JUDICIAL', 'RECUPERACIONES','Derivado a judicial CRED-2024-000050 — 135 días mora',                     (SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000050'),           '127.0.0.1'),
('gerencia@banco.pe',  'GERENCIA',      'COBRANZA_CASTIGO',  'RECUPERACIONES','Castigado CRED-2024-000001 — 200 días mora. Incobrabilidad declarada.',     (SELECT id FROM creditos WHERE numero_operacion='CRED-2024-000001'),           '127.0.0.1'),
('comite@banco.pe',    'COMITE',        'LOGIN',             'AUTH',          'Login exitoso — Core Bancario',                                              NULL,                                                                         '127.0.0.1'),
('gerencia@banco.pe',  'GERENCIA',      'LOGIN',             'AUTH',          'Login exitoso — Core Bancario',                                              NULL,                                                                         '127.0.0.1');
