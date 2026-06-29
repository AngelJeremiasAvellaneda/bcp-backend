-- ============================================================
-- Script para recrear tabla usuarios con todas las columnas
-- ============================================================

-- 1. Backup de datos existentes (si los hay)
CREATE TABLE IF NOT EXISTS usuarios_backup AS 
SELECT * FROM usuarios;

-- 2. Eliminar restricciones y tabla vieja
DROP TABLE IF EXISTS usuarios CASCADE;

-- 3. Recrear tipo ENUM si no existe
DROP TYPE IF EXISTS rol_usuario CASCADE;
CREATE TYPE rol_usuario AS ENUM (
    'CLIENTE', 'ASESOR', 'JEFE_REGIONAL', 'RIESGOS', 'COMITE', 'GERENCIA', 'ADMIN'
);

-- 4. Crear tabla nueva con todas las columnas
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

-- 5. Crear índices
CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);
CREATE INDEX idx_usuarios_dni ON usuarios(dni);
CREATE INDEX idx_usuarios_tarjeta ON usuarios(numero_tarjeta);

-- 6. Comentarios
COMMENT ON TABLE  usuarios        IS 'Usuarios del sistema: clientes (Homebanking) y personal del banco (Core)';
COMMENT ON COLUMN usuarios.rol    IS 'Rol para RBAC: CLIENTE usa Homebanking; demás roles usan el Core';
COMMENT ON COLUMN usuarios.centro_laboral IS 'Centro laboral del usuario';
COMMENT ON COLUMN usuarios.cuenta_creada IS 'Indica si la cuenta digital ya fue creada';

-- 7. Verificar estructura
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_name = 'usuarios' 
ORDER BY ordinal_position;
