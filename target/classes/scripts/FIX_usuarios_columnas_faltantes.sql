-- ============================================================
-- Script para agregar columnas faltantes a tabla usuarios
-- ============================================================

-- Verificar y agregar columnas faltantes
ALTER TABLE usuarios 
  ADD COLUMN IF NOT EXISTS centro_laboral VARCHAR(200),
  ADD COLUMN IF NOT EXISTS cuenta_creada BOOLEAN NOT NULL DEFAULT FALSE;

-- Comentarios
COMMENT ON COLUMN usuarios.centro_laboral IS 'Centro laboral del usuario';
COMMENT ON COLUMN usuarios.cuenta_creada IS 'Indica si la cuenta digital ya fue creada';

-- Crear índices si no existen
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_usuarios_rol ON usuarios(rol);
CREATE INDEX IF NOT EXISTS idx_usuarios_dni ON usuarios(dni);
CREATE INDEX IF NOT EXISTS idx_usuarios_tarjeta ON usuarios(numero_tarjeta);

-- Verificar que todo está bien
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'usuarios' 
ORDER BY ordinal_position;
