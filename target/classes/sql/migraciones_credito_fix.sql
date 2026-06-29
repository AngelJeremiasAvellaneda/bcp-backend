-- Migraciones para Fix del Módulo de Crédito
-- Aplica: Índices, validaciones, ajustes de seguridad
-- Fecha: 23/06/2026

-- ═══════════════════════════════════════════════════════════
-- ÍNDICES (Performance)
-- ═══════════════════════════════════════════════════════════

-- Índice 1: Búsqueda de cuenta por número + usuario + activa (CRÍTICO)
CREATE INDEX IF NOT EXISTS idx_cuenta_numero_usuario_activa 
ON cuentas(numero_cuenta, usuario_id, activa)
WHERE activa = true;

-- Índice 2: Búsqueda de cuentas activas por usuario
CREATE INDEX IF NOT EXISTS idx_cuenta_usuario_activa 
ON cuentas(usuario_id, activa)
WHERE activa = true;

-- Índice 3: Búsqueda de créditos por cliente y estado
CREATE INDEX IF NOT EXISTS idx_credito_cliente_estado 
ON creditos(cliente_id, estado);

-- Índice 4: Búsqueda de créditos por número de operación
CREATE INDEX IF NOT EXISTS idx_credito_numero_operacion 
ON creditos(numero_operacion);

-- Índice 5: Búsqueda de créditos por estado
CREATE INDEX IF NOT EXISTS idx_credito_estado 
ON creditos(estado);

-- ═══════════════════════════════════════════════════════════
-- VALIDACIONES (Integridad Referencial)
-- ═══════════════════════════════════════════════════════════

-- Verificar que todas las cuentas tienen usuario_id válido
SELECT COUNT(*) as cuentas_sin_usuario
FROM cuentas
WHERE usuario_id IS NULL;

-- Verificar que todos los créditos tienen cliente_id válido
SELECT COUNT(*) as creditos_sin_cliente
FROM creditos
WHERE cliente_id IS NULL;

-- Verificar que cuentas activas tienen saldo válido
SELECT COUNT(*) as cuentas_inconsistentes
FROM cuentas
WHERE activa = true AND (saldo IS NULL OR saldo < 0);

-- ═══════════════════════════════════════════════════════════
-- AUDIT: Verificar que los datos de prueba existen
-- ═══════════════════════════════════════════════════════════

-- Buscar a Angel Jeremias
SELECT u.id, u.nombre, u.email, u.rol, u.activo
FROM usuarios u
WHERE u.nombre LIKE '%Angel%' OR u.email LIKE '%angel%'
ORDER BY u.id;

-- Buscar cuentas de Angel
SELECT u.id, u.nombre, c.id, c.numero_cuenta, c.tipo_cuenta, c.saldo, c.activa
FROM usuarios u
LEFT JOIN cuentas c ON c.usuario_id = u.id
WHERE u.nombre LIKE '%Angel%' OR u.email LIKE '%angel%'
ORDER BY u.id, c.activa DESC;

-- ═══════════════════════════════════════════════════════════
-- DATA: Crear datos de prueba si no existen
-- ═══════════════════════════════════════════════════════════

-- Insertar Angel Jeremias si no existe
INSERT INTO usuarios (nombre, email, password, dni, rol, activo, created_at)
SELECT 'Angel Jeremias', 'angel@banco.pe', '$2a$10$...', '12345678', 'CLIENTE', true, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE email = 'angel@banco.pe'
);

-- Insertar cuenta de Angel si no existe
INSERT INTO cuentas (numero_cuenta, cci, tipo_cuenta, saldo, moneda, activa, usuario_id, fecha_apertura, created_at)
SELECT 
    '0099887766550001',
    '0099887766550001XXX',
    'DIGITAL',
    0.00,
    'PEN',
    true,
    u.id,
    NOW(),
    NOW()
FROM usuarios u
WHERE u.email = 'angel@banco.pe'
AND NOT EXISTS (
    SELECT 1 FROM cuentas WHERE numero_cuenta = '0099887766550001'
);

-- ═══════════════════════════════════════════════════════════
-- TEST: Verificar que la query funciona correctamente
-- ═══════════════════════════════════════════════════════════

-- Test Query 1: Buscar cuenta activa de Angel
SELECT c.id, c.numero_cuenta, c.tipo_cuenta, c.saldo, c.activa, u.nombre, u.email
FROM cuentas c
JOIN usuarios u ON c.usuario_id = u.id
WHERE c.numero_cuenta = '0099887766550001'
AND u.id = (SELECT id FROM usuarios WHERE email = 'angel@banco.pe')
AND c.activa = true;

-- Test Query 2: Verificar que no devuelve cuentas inactivas
SELECT COUNT(*) as debbe_ser_cero
FROM cuentas c
JOIN usuarios u ON c.usuario_id = u.id
WHERE c.numero_cuenta = '0099887766550001'
AND u.id = (SELECT id FROM usuarios WHERE email = 'angel@banco.pe')
AND c.activa = false;

-- Test Query 3: Verificar que no devuelve cuentas de otros usuarios
SELECT COUNT(*) as debbe_ser_cero
FROM cuentas c
WHERE c.numero_cuenta = '0099887766550001'
AND c.usuario_id != (SELECT id FROM usuarios WHERE email = 'angel@banco.pe');

-- ═══════════════════════════════════════════════════════════
-- ESTADÍSTICAS: Estado del módulo
-- ═══════════════════════════════════════════════════════════

-- Total de créditos por estado
SELECT 
    estado,
    COUNT(*) as cantidad,
    AVG(monto_solicitado) as monto_promedio,
    AVG(score_crediticio) as score_promedio
FROM creditos
GROUP BY estado
ORDER BY cantidad DESC;

-- Créditos de Angel
SELECT 
    c.numero_operacion,
    c.estado,
    c.monto_solicitado,
    c.score_crediticio,
    c.rds_ratio,
    c.rds_semaforo,
    c.created_at
FROM creditos c
JOIN usuarios u ON c.cliente_id = u.id
WHERE u.email = 'angel@banco.pe'
ORDER BY c.created_at DESC;

-- Cuentas de Angel
SELECT 
    numero_cuenta,
    tipo_cuenta,
    saldo,
    moneda,
    activa,
    fecha_apertura,
    created_at
FROM cuentas
WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'angel@banco.pe')
ORDER BY activa DESC, created_at DESC;

-- ═══════════════════════════════════════════════════════════
-- AUDIT LOG
-- ═══════════════════════════════════════════════════════════

-- Ver último evento del módulo de crédito
SELECT 
    actor,
    accion,
    modulo,
    descripcion,
    recurso_id,
    created_at
FROM auditoria_eventos
WHERE modulo = 'CREDITO'
ORDER BY created_at DESC
LIMIT 10;

