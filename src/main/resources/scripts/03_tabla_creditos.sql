-- ============================================================
-- Script 03: Tabla de créditos — Módulo Core
-- Productos: PERSONAL, HIPOTECARIO, VEHICULAR, AGROPECUARIO, MICROEMPRESA
-- Estados: SOLICITADO → EN_EVALUACION → PENDIENTE_* → APROBADO → DESEMBOLSADO
-- ============================================================

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

    -- Reglas de negocio
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

COMMENT ON TABLE  creditos                  IS 'Solicitudes y créditos otorgados — Core + Homebanking';
COMMENT ON COLUMN creditos.rds_semaforo     IS 'Verde: RDS≤30% | Amarillo: 30-50% | Rojo: >50% (no elegible)';
COMMENT ON COLUMN creditos.ruta_aprobacion  IS 'Asesor≤5k | Admin≤20k | Jefe≤50k | Riesgos≤150k | Comité>150k';
