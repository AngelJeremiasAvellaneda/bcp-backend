-- ============================================================
-- Banco Andino — Core Financiero + Homebanking
-- Script 00: Inicialización del esquema en Supabase / PostgreSQL
-- Base de datos: postgres (Supabase — proyecto eimjentsvgnmpxkxbjgf)
-- Host: aws-1-us-east-2.pooler.supabase.com:5432
-- ============================================================

-- Ejecutar contra: postgresql://postgres.eimjentsvgnmpxkxbjgf:...@aws-1-us-east-2.pooler.supabase.com:5432/postgres
-- La base de datos ya es "postgres" en Supabase (no se crea una nueva)

-- Extensiones necesarias (PostgreSQL / Supabase)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Limpiar tablas si se re-ejecuta (orden inverso por FK)
DROP TABLE IF EXISTS auditoria_eventos     CASCADE;
DROP TABLE IF EXISTS gestiones_cobranza   CASCADE;
DROP TABLE IF EXISTS cuotas_credito       CASCADE;
DROP TABLE IF EXISTS movimientos          CASCADE;
DROP TABLE IF EXISTS creditos             CASCADE;
DROP TABLE IF EXISTS cuentas             CASCADE;
DROP TABLE IF EXISTS usuarios            CASCADE;

-- Limpiar tipos si existen
DROP TYPE IF EXISTS tipo_gestion_cobranza CASCADE;
DROP TYPE IF EXISTS resultado_gestion     CASCADE;
DROP TYPE IF EXISTS estado_cuota          CASCADE;
DROP TYPE IF EXISTS ruta_aprobacion       CASCADE;
DROP TYPE IF EXISTS semaforo_rds          CASCADE;
DROP TYPE IF EXISTS estado_credito        CASCADE;
DROP TYPE IF EXISTS tipo_producto_credito CASCADE;
DROP TYPE IF EXISTS tipo_movimiento       CASCADE;
DROP TYPE IF EXISTS tipo_cuenta           CASCADE;
DROP TYPE IF EXISTS rol_usuario           CASCADE;
