-- ============================================================
-- Script 05: Módulo Recuperaciones — R2 Gestiones de cobranza
-- ============================================================

CREATE TYPE tipo_gestion_cobranza AS ENUM (
    'LLAMADA_TELEFONICA', 'SMS', 'EMAIL',
    'VISITA_DOMICILIARIA', 'CARTA_NOTARIAL', 'ACUERDO_PAGO', 'OTRO'
);

CREATE TYPE resultado_gestion AS ENUM (
    'CONTACTO_EXITOSO', 'SIN_CONTACTO', 'PROMESA_PAGO',
    'PAGO_REALIZADO', 'NEGATIVA_PAGO', 'NUMERO_INCORRECTO', 'OTRO'
);

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

COMMENT ON TABLE gestiones_cobranza IS 'R2: Historial de acciones de cobranza sobre créditos en mora';
