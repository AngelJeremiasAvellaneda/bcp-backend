-- ═══════════════════════════════════════════════════════════════════════════════
-- SCRIPT MAESTRO SUPABASE COMPLETO — Banco Confianza
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- CONSOLIDACIÓN TOTAL DE TODOS LOS SCRIPTS SQL
-- Incluye: schema, tables, fixes, datos calibrados, vistas para Power BI
--
-- INSTRUCCIONES:
-- 1. Ir a: https://supabase.com/dashboard
-- 2. SQL Editor → New Query
-- 3. Copiar TODO este contenido
-- 4. Click RUN
-- 5. Esperar a completarse
-- 6. Verificar en Data Browser que existan todas las tablas
--
-- ═══════════════════════════════════════════════════════════════════════════════

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 1: LIMPIAR SCHEMA (Drop tables + crear extensions)
-- ───────────────────────────────────────────────────────────────────────────────

DROP TABLE IF EXISTS auditoria_eventos CASCADE;
DROP TABLE IF EXISTS gestiones_cobranza CASCADE;
DROP TABLE IF EXISTS cuotas_credito CASCADE;
DROP TABLE IF EXISTS movimientos CASCADE;
DROP TABLE IF EXISTS creditos CASCADE;
DROP TABLE IF EXISTS cuentas CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;

DROP TYPE IF EXISTS rol_usuario CASCADE;
DROP TYPE IF EXISTS tipo_cuenta CASCADE;
DROP TYPE IF EXISTS tipo_movimiento CASCADE;
DROP TYPE IF EXISTS tipo_producto_credito CASCADE;
DROP TYPE IF EXISTS estado_credito CASCADE;
DROP TYPE IF EXISTS semaforo_rds CASCADE;
DROP TYPE IF EXISTS ruta_aprobacion CASCADE;
DROP TYPE IF EXISTS estado_cuota CASCADE;
DROP TYPE IF EXISTS tipo_gestion_cobranza CASCADE;
DROP TYPE IF EXISTS resultado_gestion CASCADE;
DROP TYPE IF EXISTS tipo_accion_auditoria CASCADE;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 2: CREAR TIPOS ENUM (antes que las tablas)
-- ───────────────────────────────────────────────────────────────────────────────

CREATE TYPE rol_usuario AS ENUM (
    'CLIENTE', 'ASESOR', 'JEFE_REGIONAL', 'RIESGOS', 'COMITE', 'GERENCIA', 'ADMIN'
);

CREATE TYPE tipo_cuenta AS ENUM ('AHORROS', 'CORRIENTE');

CREATE TYPE tipo_movimiento AS ENUM (
    'DEPOSITO', 'RETIRO',
    'TRANSFERENCIA_ENVIADA', 'TRANSFERENCIA_RECIBIDA',
    'PAGO_SERVICIO', 'DESEMBOLSO_CREDITO', 'CUOTA_CREDITO'
);

CREATE TYPE tipo_producto_credito AS ENUM (
    'PERSONAL', 'HIPOTECARIO', 'VEHICULAR', 'AGROPECUARIO', 'MICROEMPRESA'
);

CREATE TYPE estado_credito AS ENUM (
    'SOLICITADO', 'EN_EVALUACION',
    'PENDIENTE_ADMIN', 'PENDIENTE_JEFE_REGIONAL',
    'PENDIENTE_RIESGOS', 'PENDIENTE_COMITE',
    'APROBADO', 'DESEMBOLSADO', 'RECHAZADO', 'CANCELADO'
);

CREATE TYPE semaforo_rds AS ENUM ('VERDE', 'AMARILLO', 'ROJO');

CREATE TYPE ruta_aprobacion AS ENUM (
    'ASESOR', 'ADMIN', 'JEFE_REGIONAL', 'RIESGOS', 'COMITE'
);

CREATE TYPE estado_cuota AS ENUM ('PENDIENTE', 'PAGADA', 'VENCIDA', 'PARCIAL');

CREATE TYPE tipo_gestion_cobranza AS ENUM (
    'LLAMADA_TELEFONICA', 'SMS', 'EMAIL',
    'VISITA_DOMICILIARIA', 'CARTA_NOTARIAL', 'ACUERDO_PAGO', 'OTRO'
);

CREATE TYPE resultado_gestion AS ENUM (
    'CONTACTO_EXITOSO', 'SIN_CONTACTO', 'PROMESA_PAGO',
    'PAGO_REALIZADO', 'NEGATIVA_PAGO', 'NUMERO_INCORRECTO', 'OTRO'
);

CREATE TYPE tipo_accion_auditoria AS ENUM (
    'LOGIN', 'LOGOUT',
    'CREDITO_SOLICITUD', 'CREDITO_APROBACION', 'CREDITO_RECHAZO', 'CREDITO_DESEMBOLSO',
    'CUENTA_TRANSFERENCIA', 'CUENTA_DEPOSITO', 'CUENTA_RETIRO',
    'COBRANZA_GESTION', 'COBRANZA_JUDICIAL', 'COBRANZA_CASTIGO',
    'USUARIO_CREACION', 'USUARIO_EDICION'
);

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 3: TABLA USUARIOS
-- ───────────────────────────────────────────────────────────────────────────────

CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(200) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    dni             VARCHAR(8) UNIQUE,
    numero_tarjeta  VARCHAR(16) UNIQUE,
    telefono        VARCHAR(15),
    fecha_nacimiento DATE,
    direccion       VARCHAR(255),
    ocupacion       VARCHAR(100),
    profesion       VARCHAR(100),
    centro_laboral  VARCHAR(200),
    rol             rol_usuario  NOT NULL DEFAULT 'CLIENTE',
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    cuenta_creada   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_rol   ON usuarios(rol);
CREATE INDEX idx_usuarios_dni   ON usuarios(dni);
CREATE INDEX idx_usuarios_tarjeta ON usuarios(numero_tarjeta);

COMMENT ON TABLE  usuarios        IS 'Usuarios del sistema: clientes (Homebanking) y personal del banco (Core)';
COMMENT ON COLUMN usuarios.rol    IS 'Rol para RBAC: CLIENTE usa Homebanking; demás roles usan el Core';

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 4: TABLA CUENTAS
-- ───────────────────────────────────────────────────────────────────────────────

CREATE TABLE cuentas (
    id             BIGSERIAL PRIMARY KEY,
    numero_cuenta  VARCHAR(20)     NOT NULL UNIQUE,
    tipo           tipo_cuenta     NOT NULL DEFAULT 'AHORROS',
    saldo          NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    moneda         CHAR(3)         NOT NULL DEFAULT 'PEN',
    activa         BOOLEAN         NOT NULL DEFAULT TRUE,
    usuario_id     BIGINT          NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_saldo_no_negativo CHECK (saldo >= 0)
);

CREATE INDEX idx_cuentas_usuario_id    ON cuentas(usuario_id);
CREATE INDEX idx_cuentas_numero_cuenta ON cuentas(numero_cuenta);

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 5: TABLA MOVIMIENTOS
-- ───────────────────────────────────────────────────────────────────────────────

CREATE TABLE movimientos (
    id               BIGSERIAL PRIMARY KEY,
    cuenta_id        BIGINT          NOT NULL REFERENCES cuentas(id) ON DELETE CASCADE,
    tipo             tipo_movimiento NOT NULL,
    monto            NUMERIC(15,2)   NOT NULL,
    saldo_anterior   NUMERIC(15,2)   NOT NULL,
    saldo_posterior  NUMERIC(15,2)   NOT NULL,
    descripcion      VARCHAR(255),
    cuenta_destino   VARCHAR(20),
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_monto_positivo CHECK (monto > 0)
);

CREATE INDEX idx_movimientos_cuenta_id  ON movimientos(cuenta_id);
CREATE INDEX idx_movimientos_created_at ON movimientos(created_at DESC);

COMMENT ON TABLE movimientos IS 'Registro inmutable de todas las operaciones monetarias';

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 6: TABLA CRÉDITOS
-- ───────────────────────────────────────────────────────────────────────────────

CREATE TABLE creditos (
    id                    BIGSERIAL PRIMARY KEY,
    numero_operacion      VARCHAR(30)            NOT NULL UNIQUE,
    cliente_id            BIGINT                 NOT NULL REFERENCES usuarios(id),
    asesor_id             BIGINT                 REFERENCES usuarios(id),
    tipo_producto         tipo_producto_credito  NOT NULL,
    monto_solicitado      NUMERIC(15,2)          NOT NULL,
    monto_aprobado        NUMERIC(15,2),
    tea                   NUMERIC(6,4),
    plazo_meses           INTEGER                NOT NULL,
    cuota_mensual         NUMERIC(15,2),
    moneda                CHAR(3)                NOT NULL DEFAULT 'PEN',
    proposito             VARCHAR(500),

    -- Evaluación crediticia
    score_crediticio      INTEGER                CHECK (score_crediticio BETWEEN 0 AND 1000),
    ingreso_mensual       NUMERIC(15,2),
    deuda_total_vigente   NUMERIC(15,2)          DEFAULT 0,
    rds_ratio             NUMERIC(6,4),
    rds_semaforo          semaforo_rds,
    es_sujeto_credito     BOOLEAN,

    -- Flujo de aprobación
    estado                estado_credito         NOT NULL DEFAULT 'SOLICITADO',
    aprobado_por_id       BIGINT                 REFERENCES usuarios(id),
    comentario_evaluacion VARCHAR(1000),
    ruta_aprobacion       ruta_aprobacion,

    -- Desembolso
    cuenta_desembolso_id  BIGINT                 REFERENCES cuentas(id),
    fecha_desembolso      DATE,
    fecha_primera_cuota   DATE,
    fecha_ultima_cuota    DATE,

    created_at            TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_monto_minimo    CHECK (monto_solicitado >= 500),
    CONSTRAINT chk_plazo_minimo    CHECK (plazo_meses >= 3),
    CONSTRAINT chk_plazo_maximo    CHECK (plazo_meses <= 360)
);

CREATE INDEX idx_creditos_cliente_id ON creditos(cliente_id);
CREATE INDEX idx_creditos_estado     ON creditos(estado);
CREATE INDEX idx_creditos_asesor_id  ON creditos(asesor_id);

-- Trigger para updated_at automático
CREATE OR REPLACE FUNCTION actualizar_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = CURRENT_TIMESTAMP; RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_creditos_updated_at
    BEFORE UPDATE ON creditos
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 7: TABLA CUOTAS_CREDITO
-- ───────────────────────────────────────────────────────────────────────────────

CREATE TABLE cuotas_credito (
    id                BIGSERIAL PRIMARY KEY,
    credito_id        BIGINT          NOT NULL REFERENCES creditos(id) ON DELETE CASCADE,
    numero_cuota      INTEGER         NOT NULL,
    fecha_vencimiento DATE            NOT NULL,
    fecha_pago        DATE,
    capital           NUMERIC(15,2)   NOT NULL,
    interes           NUMERIC(15,2)   NOT NULL,
    seguro            NUMERIC(15,2)   NOT NULL DEFAULT 0,
    cuota_total       NUMERIC(15,2)   NOT NULL,
    saldo_capital     NUMERIC(15,2)   NOT NULL,
    dias_mora         INTEGER         NOT NULL DEFAULT 0,
    monto_mora        NUMERIC(15,2)   NOT NULL DEFAULT 0,
    estado_cuota      estado_cuota    NOT NULL DEFAULT 'PENDIENTE',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_cuota_numero_positivo CHECK (numero_cuota > 0),
    CONSTRAINT chk_dias_mora_positivo    CHECK (dias_mora >= 0),
    UNIQUE(credito_id, numero_cuota)
);

CREATE INDEX idx_cuotas_credito_id        ON cuotas_credito(credito_id);
CREATE INDEX idx_cuotas_estado            ON cuotas_credito(estado_cuota);
CREATE INDEX idx_cuotas_fecha_vencimiento ON cuotas_credito(fecha_vencimiento);

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 8: TABLA GESTIONES_COBRANZA
-- ───────────────────────────────────────────────────────────────────────────────

CREATE TABLE gestiones_cobranza (
    id                      BIGSERIAL PRIMARY KEY,
    credito_id              BIGINT                  NOT NULL REFERENCES creditos(id),
    cuota_id                BIGINT                  REFERENCES cuotas_credito(id),
    gestor_id               BIGINT                  NOT NULL REFERENCES usuarios(id),
    tipo_gestion            tipo_gestion_cobranza   NOT NULL,
    resultado               resultado_gestion       NOT NULL,
    descripcion             VARCHAR(2000),
    fecha_compromiso_pago   DATE,
    dias_mora_al_gestionar  INTEGER                 DEFAULT 0,
    created_at              TIMESTAMP               NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gestiones_credito_id ON gestiones_cobranza(credito_id);
CREATE INDEX idx_gestiones_gestor_id  ON gestiones_cobranza(gestor_id);

-- ───────────────────────────────────────────────────────────────────────────────
-- PASO 9: TABLA AUDITORIA_EVENTOS
-- ───────────────────────────────────────────────────────────────────────────────

CREATE TABLE auditoria_eventos (
    id           BIGSERIAL PRIMARY KEY,
    email_actor  VARCHAR(150),
    rol_actor    VARCHAR(30),
    accion       tipo_accion_auditoria NOT NULL,
    modulo       VARCHAR(50),
    descripcion  VARCHAR(500),
    recurso_id   BIGINT,
    ip_origen    VARCHAR(50),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auditoria_email_actor ON auditoria_eventos(email_actor);
CREATE INDEX idx_auditoria_accion      ON auditoria_eventos(accion);
CREATE INDEX idx_auditoria_created_at  ON auditoria_eventos(created_at DESC);

COMMENT ON TABLE auditoria_eventos IS 'Registro inmutable de todas las acciones del sistema — trazabilidad RBAC';

-- ═══════════════════════════════════════════════════════════════════════════════
-- PASO 10: INSERTAR DATOS CALIBRADOS
-- ═══════════════════════════════════════════════════════════════════════════════

-- Usuarios clientes
INSERT INTO usuarios (nombre, email, password, rol, activo) VALUES
('Ana García López',          'demo@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Pedro Sánchez Ruiz',        'pedro@banco.pe',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Lucía Ramírez Torres',      'lucia@banco.pe',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Miguel Quispe Huanca',      'miguel@banco.pe',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Carmen Flores Vidal',       'carmen@banco.pe',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('José Mamani Ccopa',         'jose@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Rosa Condori Apaza',        'rosa@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true),
('Luis Vargas Medina',        'luis@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CLIENTE',       true);

-- Personal Core (roles)
INSERT INTO usuarios (nombre, email, password, rol, activo) VALUES
('Carlos Mendoza Vega',       'asesor@banco.pe',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ASESOR',        true),
('Sofía Paredes Luna',        'asesor2@banco.pe',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ASESOR',        true),
('María Torres Silva',        'admin@banco.pe',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN',         true),
('Roberto Castillo Díaz',     'jefe@banco.pe',       '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'JEFE_REGIONAL', true),
('Laura Fernández Ortiz',     'riesgos@banco.pe',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'RIESGOS',       true),
('Miguel Ángel Paredes',      'comite@banco.pe',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'COMITE',        true),
('Dr. Jorge Villanueva Reyes','gerencia@banco.pe',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'GERENCIA',      true);

-- Cuentas (9 total)
INSERT INTO cuentas (numero_cuenta, tipo, saldo, moneda, activa, usuario_id) VALUES
('0011223344550001', 'AHORROS',   4850.00, 'PEN', true, 1),
('0011223344550002', 'CORRIENTE', 1200.50, 'PEN', true, 1),
('0022334455660001', 'AHORROS',    750.00, 'PEN', true, 2),
('0033445566770001', 'AHORROS',   3200.00, 'PEN', true, 3),
('0044556677880001', 'AHORROS',    500.00, 'PEN', true, 4),
('0055667788990001', 'AHORROS',   1800.00, 'PEN', true, 5),
('0066778899000001', 'AHORROS',    200.00, 'PEN', true, 6),
('0077889900110001', 'AHORROS',      0.00, 'PEN', true, 7),
('0088990011220001', 'AHORROS',    950.00, 'PEN', true, 8);

-- Movimientos iniciales (Homebanking)
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
(1, 'DEPOSITO',               2000.00, 2850.00, 4850.00, 'Depósito inicial apertura'),
(1, 'PAGO_SERVICIO',            185.40, 4850.00, 4664.60, 'Luz del Sur — Mayo 2026'),
(1, 'PAGO_SERVICIO',             89.90, 4664.60, 4574.70, 'Claro Internet — Mayo 2026'),
(1, 'TRANSFERENCIA_ENVIADA',    500.00, 4574.70, 4074.70, 'Envío a Pedro Sánchez'),
(2, 'TRANSFERENCIA_RECIBIDA',   500.00,  250.00,  750.00, 'Recibido de Ana García'),
(2, 'DEPOSITO',               1200.50,    0.00, 1200.50, 'Ingreso sueldo Mayo 2026'),
(3, 'DEPOSITO',               3200.00,    0.00, 3200.00, 'Depósito inicial'),
(5, 'DEPOSITO',               1800.00,    0.00, 1800.00, 'Depósito salario');

-- Créditos iniciales (3 básicos)
INSERT INTO creditos (
    numero_operacion, cliente_id, asesor_id, tipo_producto,
    monto_solicitado, monto_aprobado, tea, plazo_meses, cuota_mensual, moneda,
    proposito, score_crediticio, ingreso_mensual, deuda_total_vigente,
    rds_ratio, rds_semaforo, es_sujeto_credito,
    estado, aprobado_por_id, comentario_evaluacion, ruta_aprobacion,
    cuenta_desembolso_id, fecha_desembolso, fecha_primera_cuota, fecha_ultima_cuota
) VALUES
('CRED-2026-000001', 1, 10, 'PERSONAL', 5000.00, 5000.00, 0.3500, 24, 283.75, 'PEN',
 'Capital de trabajo personal', 750, 2500.00, 283.75, 0.1135, 'VERDE', true,
 'DESEMBOLSADO', 10, 'Aprobado automáticamente — Score 750, RDS 11%', 'ASESOR',
 1, CURRENT_DATE - INTERVAL '3 months', CURRENT_DATE - INTERVAL '2 months', CURRENT_DATE + INTERVAL '22 months'),

('CRED-2026-000002', 1, 10, 'HIPOTECARIO', 80000.00, 80000.00, 0.0850, 240, 696.04, 'PEN',
 'Adquisición de vivienda — Conjunto Los Pinos, Miraflores', 820, 4500.00, 696.04, 0.2177, 'VERDE', true,
 'PENDIENTE_JEFE_REGIONAL', NULL, 'Evaluado por asesor. Score 820. RDS proyectado 21.8% — VERDE.', 'JEFE_REGIONAL',
 1, NULL, NULL, NULL),

('CRED-2026-000003', 3, 11, 'VEHICULAR', 15000.00, 15000.00, 0.1600, 48, 425.40, 'PEN',
 'Adquisición vehículo familiar — Toyota Yaris 2025', 680, 3000.00, 425.40, 0.1418, 'VERDE', true,
 'APROBADO', 11, 'Aprobado por Administración. Score 680, RDS 14.2% VERDE.', 'ADMIN',
 3, NULL, NULL, NULL);

-- Movimientos de desembolso
INSERT INTO movimientos (cuenta_id, tipo, monto, saldo_anterior, saldo_posterior, descripcion) VALUES
(1, 'DESEMBOLSO_CREDITO', 5000.00, 4850.00, 9850.00, 'Desembolso crédito CRED-2026-000001'),
(3, 'DESEMBOLSO_CREDITO', 15000.00, 3200.00, 18200.00, 'Desembolso crédito CRED-2026-000003');

-- Cuotas CRED-2026-000001 (3 cuotas: 2 pagadas + 1 pendiente)
INSERT INTO cuotas_credito (credito_id, numero_cuota, fecha_vencimiento, fecha_pago, capital, interes, seguro, cuota_total, saldo_capital, dias_mora, monto_mora, estado_cuota) VALUES
(1, 1, CURRENT_DATE - INTERVAL '2 months', CURRENT_DATE - INTERVAL '2 months', 208.33, 75.42, 0, 283.75, 4791.67, 0, 0.00, 'PAGADA'),
(1, 2, CURRENT_DATE - INTERVAL '1 month',  CURRENT_DATE - INTERVAL '1 month',  211.30, 72.45, 0, 283.75, 4580.37, 0, 0.00, 'PAGADA'),
(1, 3, CURRENT_DATE + INTERVAL '10 days',  NULL,                               214.31, 69.44, 0, 283.75, 4366.06, 0, 0.00, 'PENDIENTE');

-- ═══════════════════════════════════════════════════════════════════════════════
-- PASO 11: CREAR VISTAS PARA POWER BI
-- ═══════════════════════════════════════════════════════════════════════════════

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
    c.created_at                                        AS fecha_solicitud
FROM creditos c
JOIN usuarios uc ON uc.id = c.cliente_id
LEFT JOIN usuarios ua ON ua.id = c.asesor_id;

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
    m.created_at                                        AS fecha_operacion
FROM movimientos m
JOIN cuentas ct ON ct.id = m.cuenta_id
JOIN usuarios u ON u.id = ct.usuario_id;

-- ═══════════════════════════════════════════════════════════════════════════════
-- ✅ FIN — SCHEMA COMPLETAMENTE INICIALIZADO
-- ═══════════════════════════════════════════════════════════════════════════════

SELECT '✅ SCHEMA SUPABASE COMPLETAMENTE CONFIGURADO' AS resultado;

-- Verificación final
SELECT 
    (SELECT COUNT(*) FROM usuarios) as total_usuarios,
    (SELECT COUNT(*) FROM cuentas) as total_cuentas,
    (SELECT COUNT(*) FROM creditos) as total_creditos,
    (SELECT COUNT(*) FROM cuotas_credito) as total_cuotas,
    (SELECT COUNT(*) FROM movimientos) as total_movimientos;
