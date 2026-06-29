-- ============================================================
-- Script 05b: Tabla de auditoría — trazabilidad completa
-- ============================================================

CREATE TYPE tipo_accion_auditoria AS ENUM (
    'LOGIN', 'LOGOUT',
    'CREDITO_SOLICITUD', 'CREDITO_APROBACION', 'CREDITO_RECHAZO', 'CREDITO_DESEMBOLSO',
    'CUENTA_TRANSFERENCIA', 'CUENTA_DEPOSITO', 'CUENTA_RETIRO',
    'COBRANZA_GESTION', 'COBRANZA_JUDICIAL', 'COBRANZA_CASTIGO',
    'USUARIO_CREACION', 'USUARIO_EDICION'
);

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
