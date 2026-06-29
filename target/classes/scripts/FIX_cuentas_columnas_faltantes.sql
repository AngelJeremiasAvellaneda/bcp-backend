-- ============================================================
-- Script para agregar columnas faltantes a tabla cuentas
-- ============================================================

-- Agregar columnas faltantes
ALTER TABLE cuentas 
ADD COLUMN IF NOT EXISTS cci VARCHAR(20) UNIQUE,
ADD COLUMN IF NOT EXISTS fecha_apertura TIMESTAMP,
ADD COLUMN IF NOT EXISTS tipo_cuenta VARCHAR(50);

-- Si tipo_cuenta no existe pero tipo sí, copiar datos
UPDATE cuentas SET tipo_cuenta = tipo WHERE tipo_cuenta IS NULL;

-- Verificar estructura actual
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'cuentas' 
ORDER BY ordinal_position;
