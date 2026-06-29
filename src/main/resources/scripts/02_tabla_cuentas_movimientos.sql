-- ============================================================
-- Script 02: Cuentas bancarias y movimientos
-- ============================================================

CREATE TYPE tipo_cuenta AS ENUM ('AHORROS', 'CORRIENTE');

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

-- ── Movimientos ────────────────────────────────────────────

CREATE TYPE tipo_movimiento AS ENUM (
    'DEPOSITO', 'RETIRO',
    'TRANSFERENCIA_ENVIADA', 'TRANSFERENCIA_RECIBIDA',
    'PAGO_SERVICIO', 'DESEMBOLSO_CREDITO', 'CUOTA_CREDITO'
);

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
