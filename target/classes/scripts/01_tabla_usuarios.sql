-- ============================================================
-- Script 01: Tabla de usuarios con RBAC expandido
-- Roles: CLIENTE, ASESOR, JEFE_REGIONAL, RIESGOS, COMITE, GERENCIA, ADMIN
-- ============================================================

CREATE TYPE rol_usuario AS ENUM (
    'CLIENTE', 'ASESOR', 'JEFE_REGIONAL', 'RIESGOS', 'COMITE', 'GERENCIA', 'ADMIN'
);

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
