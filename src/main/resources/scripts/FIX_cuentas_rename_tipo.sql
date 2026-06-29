-- ============================================================
-- Script para arreglar la columna tipo -> tipo_cuenta
-- ============================================================

-- 1. Renombrar columna tipo a tipo_cuenta
ALTER TABLE cuentas RENAME COLUMN tipo TO tipo_cuenta;

-- 2. Asegurar que tipo_cuenta es NOT NULL
ALTER TABLE cuentas ALTER COLUMN tipo_cuenta SET NOT NULL;
ALTER TABLE cuentas ALTER COLUMN tipo_cuenta SET DEFAULT 'AHORROS';

-- 3. Actualizar los valores NULL a AHORROS si los hay
UPDATE cuentas SET tipo_cuenta = 'AHORROS' WHERE tipo_cuenta IS NULL;

-- 4. Crear índices si no existen
CREATE INDEX IF NOT EXISTS idx_cuentas_usuario_id ON cuentas(usuario_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_numero_cuenta ON cuentas(numero_cuenta);

-- 5. Verificar estructura final
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_name = 'cuentas' 
ORDER BY ordinal_position;
