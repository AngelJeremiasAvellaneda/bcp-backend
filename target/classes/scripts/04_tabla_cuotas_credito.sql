-- ============================================================
-- Script 04: Cronograma de cuotas (sistema francés)
-- ============================================================

CREATE TYPE estado_cuota AS ENUM ('PENDIENTE', 'PAGADA', 'VENCIDA', 'PARCIAL');

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

COMMENT ON TABLE  cuotas_credito             IS 'Cronograma de pagos francés: cuota=capital+interés constante';
COMMENT ON COLUMN cuotas_credito.dias_mora   IS 'Días desde fecha_vencimiento hasta fecha actual (si no pagada)';
COMMENT ON COLUMN cuotas_credito.monto_mora  IS '0.1% del capital por día de mora';
