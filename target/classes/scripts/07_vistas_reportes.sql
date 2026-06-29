-- ============================================================
-- Script 07: Vistas para Power BI + reportes de gestión
-- Conectar Power BI con:
--   Server:   aws-1-us-east-2.pooler.supabase.com
--   Port:     5432
--   Database: postgres
--   User:     postgres.eimjentsvgnmpxkxbjgf
--   Password: (ver backend/.env)
--   SSL:      Required
-- ============================================================

-- ── V1: Resumen completo de cartera de créditos ──────────────
CREATE OR REPLACE VIEW v_resumen_cartera AS
SELECT
    c.id                                                AS credito_id,
    c.numero_operacion,
    uc.nombre                                           AS cliente,
    uc.email                                            AS email_cliente,
    ua.nombre                                           AS asesor,
    c.tipo_producto,
    c.monto_solicitado,
    c.monto_aprobado,
    ROUND(c.tea * 100, 2)                               AS tea_pct,
    c.plazo_meses,
    c.cuota_mensual,
    c.estado,
    c.score_crediticio,
    ROUND(c.rds_ratio * 100, 2)                         AS rds_pct,
    c.rds_semaforo,
    c.ruta_aprobacion,
    c.es_sujeto_credito,
    c.fecha_desembolso,
    c.fecha_primera_cuota,
    c.fecha_ultima_cuota,
    c.created_at                                        AS fecha_solicitud,
    CASE
        WHEN c.estado = 'DESEMBOLSADO' THEN 'Activo'
        WHEN c.estado IN ('SOLICITADO','EN_EVALUACION','PENDIENTE_ADMIN',
             'PENDIENTE_JEFE_REGIONAL','PENDIENTE_RIESGOS','PENDIENTE_COMITE') THEN 'En flujo'
        WHEN c.estado = 'APROBADO'  THEN 'Aprobado'
        WHEN c.estado = 'RECHAZADO' THEN 'Rechazado / Castigado'
        ELSE c.estado::text
    END                                                 AS estado_agrupado
FROM creditos c
JOIN usuarios uc ON uc.id = c.cliente_id
LEFT JOIN usuarios ua ON ua.id = c.asesor_id;

-- ── V2: Cartera morosa con bandas y KPIs ─────────────────────
CREATE OR REPLACE VIEW v_cartera_morosa AS
SELECT
    c.id                                                AS credito_id,
    c.numero_operacion,
    uc.nombre                                           AS cliente,
    uc.email                                            AS email_cliente,
    c.tipo_producto,
    c.monto_aprobado,
    c.cuota_mensual,
    MAX(cc.dias_mora)                                   AS dias_mora_max,
    SUM(cc.monto_mora)                                  AS mora_total_acumulada,
    COUNT(CASE WHEN cc.estado_cuota = 'VENCIDA'  THEN 1 END) AS cuotas_vencidas,
    COUNT(CASE WHEN cc.estado_cuota = 'PENDIENTE' THEN 1 END) AS cuotas_pendientes,
    CASE
        WHEN MAX(cc.dias_mora) = 0    THEN 'AL_DIA'
        WHEN MAX(cc.dias_mora) <= 30  THEN 'PREVENTIVA'
        WHEN MAX(cc.dias_mora) <= 60  THEN 'TEMPRANA'
        WHEN MAX(cc.dias_mora) <= 120 THEN 'TARDIA'
        WHEN MAX(cc.dias_mora) <= 180 THEN 'JUDICIAL'
        ELSE                               'CASTIGO'
    END                                                 AS banda_mora,
    CASE
        WHEN MAX(cc.dias_mora) = 0    THEN 1
        WHEN MAX(cc.dias_mora) <= 30  THEN 2
        WHEN MAX(cc.dias_mora) <= 60  THEN 3
        WHEN MAX(cc.dias_mora) <= 120 THEN 4
        WHEN MAX(cc.dias_mora) <= 180 THEN 5
        ELSE                               6
    END                                                 AS banda_orden
FROM creditos c
JOIN usuarios uc ON uc.id = c.cliente_id
JOIN cuotas_credito cc ON cc.credito_id = c.id
WHERE cc.estado_cuota IN ('VENCIDA', 'PARCIAL')
GROUP BY c.id, c.numero_operacion, uc.nombre, uc.email, c.tipo_producto, c.monto_aprobado, c.cuota_mensual;

-- ── V3: KPIs de mora por banda (para tarjetas Power BI) ──────
CREATE OR REPLACE VIEW v_kpi_mora_por_banda AS
SELECT
    banda_mora,
    banda_orden,
    COUNT(*)                                            AS cantidad_creditos,
    COALESCE(SUM(monto_aprobado), 0)                   AS saldo_total,
    ROUND(AVG(dias_mora_max), 1)                        AS dias_mora_promedio,
    COALESCE(SUM(mora_total_acumulada), 0)              AS mora_acumulada_total
FROM v_cartera_morosa
GROUP BY banda_mora, banda_orden
ORDER BY banda_orden;

-- ── V4: KPI global de mora (tarjeta resumen) ─────────────────
CREATE OR REPLACE VIEW v_kpi_global_mora AS
SELECT
    COUNT(DISTINCT CASE WHEN c.estado = 'DESEMBOLSADO' THEN c.id END)
                                                        AS total_creditos_activos,
    COUNT(DISTINCT vm.credito_id)                       AS creditos_en_mora,
    ROUND(
        COUNT(DISTINCT vm.credito_id)::numeric /
        NULLIF(COUNT(DISTINCT CASE WHEN c.estado = 'DESEMBOLSADO' THEN c.id END), 0) * 100
    , 1)                                                AS tasa_mora_pct,
    COALESCE(SUM(vm.monto_aprobado), 0)                 AS saldo_en_mora,
    COALESCE(SUM(vm.mora_total_acumulada), 0)           AS mora_total_acumulada
FROM creditos c
LEFT JOIN v_cartera_morosa vm ON vm.credito_id = c.id;

-- ── V5: Bandeja de aprobaciones pendientes ───────────────────
CREATE OR REPLACE VIEW v_bandeja_aprobaciones AS
SELECT
    c.id,
    c.numero_operacion,
    c.estado,
    c.ruta_aprobacion,
    uc.nombre                                           AS cliente,
    ua.nombre                                           AS asesor,
    c.tipo_producto,
    c.monto_solicitado,
    c.score_crediticio,
    c.rds_semaforo,
    ROUND(c.rds_ratio * 100, 2)                         AS rds_pct,
    c.es_sujeto_credito,
    c.created_at                                        AS fecha_solicitud,
    EXTRACT(DAY FROM NOW() - c.created_at)::INT        AS dias_en_bandeja
FROM creditos c
JOIN usuarios uc ON uc.id = c.cliente_id
LEFT JOIN usuarios ua ON ua.id = c.asesor_id
WHERE c.estado NOT IN ('APROBADO', 'RECHAZADO', 'CANCELADO', 'DESEMBOLSADO')
ORDER BY c.created_at ASC;

-- ── V6: Cronograma completo de cuotas por crédito ────────────
CREATE OR REPLACE VIEW v_cronograma_cuotas AS
SELECT
    c.numero_operacion,
    uc.nombre                                           AS cliente,
    c.tipo_producto,
    c.monto_aprobado,
    cc.numero_cuota,
    cc.fecha_vencimiento,
    cc.fecha_pago,
    cc.capital,
    cc.interes,
    cc.cuota_total,
    cc.saldo_capital,
    cc.dias_mora,
    cc.monto_mora,
    cc.estado_cuota,
    CASE
        WHEN cc.dias_mora = 0    THEN 'AL_DIA'
        WHEN cc.dias_mora <= 30  THEN 'PREVENTIVA'
        WHEN cc.dias_mora <= 60  THEN 'TEMPRANA'
        WHEN cc.dias_mora <= 120 THEN 'TARDIA'
        WHEN cc.dias_mora <= 180 THEN 'JUDICIAL'
        ELSE                          'CASTIGO'
    END                                                 AS banda_mora
FROM cuotas_credito cc
JOIN creditos c ON c.id = cc.credito_id
JOIN usuarios uc ON uc.id = c.cliente_id
ORDER BY c.id, cc.numero_cuota;

-- ── V7: Historial de movimientos por cuenta/cliente ──────────
CREATE OR REPLACE VIEW v_movimientos_cliente AS
SELECT
    u.nombre                                            AS cliente,
    u.email,
    ct.numero_cuenta,
    ct.tipo                                             AS tipo_cuenta,
    ct.saldo                                            AS saldo_actual,
    m.tipo                                              AS tipo_movimiento,
    m.monto,
    m.saldo_anterior,
    m.saldo_posterior,
    m.descripcion,
    m.cuenta_destino,
    m.created_at                                        AS fecha_operacion,
    DATE_TRUNC('month', m.created_at)                  AS mes_operacion
FROM movimientos m
JOIN cuentas ct ON ct.id = m.cuenta_id
JOIN usuarios u ON u.id = ct.usuario_id
ORDER BY m.created_at DESC;

-- ── V8: Gestiones de cobranza con detalle ────────────────────
CREATE OR REPLACE VIEW v_gestiones_cobranza AS
SELECT
    gc.id                                               AS gestion_id,
    c.numero_operacion,
    uc.nombre                                           AS cliente,
    ug.nombre                                           AS gestor,
    ug.rol                                              AS rol_gestor,
    gc.tipo_gestion,
    gc.resultado,
    gc.descripcion,
    gc.dias_mora_al_gestionar,
    CASE
        WHEN gc.dias_mora_al_gestionar = 0    THEN 'AL_DIA'
        WHEN gc.dias_mora_al_gestionar <= 30  THEN 'PREVENTIVA'
        WHEN gc.dias_mora_al_gestionar <= 60  THEN 'TEMPRANA'
        WHEN gc.dias_mora_al_gestionar <= 120 THEN 'TARDIA'
        WHEN gc.dias_mora_al_gestionar <= 180 THEN 'JUDICIAL'
        ELSE                                       'CASTIGO'
    END                                                 AS banda_al_gestionar,
    gc.fecha_compromiso_pago,
    gc.created_at                                       AS fecha_gestion
FROM gestiones_cobranza gc
JOIN creditos c ON c.id = gc.credito_id
JOIN usuarios uc ON uc.id = c.cliente_id
JOIN usuarios ug ON ug.id = gc.gestor_id
ORDER BY gc.created_at DESC;

-- ── V9: Auditoría — trazabilidad por actor y módulo ──────────
CREATE OR REPLACE VIEW v_auditoria AS
SELECT
    ae.id,
    ae.email_actor,
    ae.rol_actor,
    ae.accion,
    ae.modulo,
    ae.descripcion,
    ae.recurso_id,
    ae.ip_origen,
    ae.created_at,
    DATE_TRUNC('hour', ae.created_at)                  AS hora_operacion
FROM auditoria_eventos ae
ORDER BY ae.created_at DESC;

-- ── V10: Resumen por tipo de producto ────────────────────────
CREATE OR REPLACE VIEW v_resumen_por_producto AS
SELECT
    tipo_producto,
    COUNT(*)                                            AS total_solicitudes,
    COUNT(CASE WHEN estado = 'DESEMBOLSADO' THEN 1 END) AS desembolsados,
    COUNT(CASE WHEN estado = 'RECHAZADO'    THEN 1 END) AS rechazados,
    COUNT(CASE WHEN estado NOT IN ('DESEMBOLSADO','RECHAZADO','CANCELADO','APROBADO') THEN 1 END) AS en_proceso,
    COALESCE(SUM(CASE WHEN estado = 'DESEMBOLSADO' THEN monto_aprobado END), 0) AS monto_desembolsado,
    ROUND(AVG(score_crediticio), 0)                    AS score_promedio,
    ROUND(AVG(CASE WHEN estado = 'DESEMBOLSADO' THEN rds_ratio END) * 100, 1) AS rds_promedio_pct
FROM creditos
GROUP BY tipo_producto
ORDER BY monto_desembolsado DESC;

-- ── Comentarios ───────────────────────────────────────────────
COMMENT ON VIEW v_resumen_cartera        IS 'Power BI: Vista completa de cartera — tablas y gráficos de estados';
COMMENT ON VIEW v_cartera_morosa         IS 'Power BI R1: Cartera en mora por bandas';
COMMENT ON VIEW v_kpi_mora_por_banda     IS 'Power BI R1: KPIs por banda para tarjetas y gráficos de barras';
COMMENT ON VIEW v_kpi_global_mora        IS 'Power BI: Tasa de mora global para KPI card';
COMMENT ON VIEW v_bandeja_aprobaciones   IS 'Power BI Core: Solicitudes pendientes por rol';
COMMENT ON VIEW v_cronograma_cuotas      IS 'Power BI: Cronograma de cuotas con mora para análisis temporal';
COMMENT ON VIEW v_movimientos_cliente    IS 'Power BI Homebanking: Operaciones por cliente y mes';
COMMENT ON VIEW v_gestiones_cobranza     IS 'Power BI R2: Gestiones de cobranza con resultado';
COMMENT ON VIEW v_auditoria              IS 'Power BI: Trazabilidad de acciones por actor y módulo';
COMMENT ON VIEW v_resumen_por_producto   IS 'Power BI: Comparativa por tipo de producto crediticio';
