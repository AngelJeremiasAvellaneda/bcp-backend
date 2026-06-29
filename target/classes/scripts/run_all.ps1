# =============================================================================
# run_all.ps1  -- Ejecuta todos los scripts SQL contra Supabase (PostgreSQL)
#
# REQUISITO: psql instalado (viene con PostgreSQL)
#   winget install PostgreSQL.PostgreSQL
#   O: https://www.enterprisedb.com/downloads/postgres-postgresql-installers
#   Agregar al PATH: C:\Program Files\PostgreSQL\17\bin
#
# USO (desde la carpeta scripts\):
#   .\run_all.ps1
#
# Para no escribir password en cada script:
#   $env:PGPASSWORD = "0Yyqed5aF0OXLcbk" ; .\run_all.ps1
# =============================================================================

$DB_HOST = "aws-1-us-east-2.pooler.supabase.com"
$DB_PORT = "5432"
$DB_NAME = "postgres"
$DB_USER = "postgres.eimjentsvgnmpxkxbjgf"

# -- Lee la password desde backend\.env si no esta en la variable de entorno --
if (-not $env:PGPASSWORD) {
    $envFile = Join-Path $PSScriptRoot "..\..\..\..\..\.env"
    # Ruta alternativa relativa al backend
    $envFile2 = Join-Path $PSScriptRoot "..\..\..\..\.env"
    foreach ($ef in @($envFile, $envFile2)) {
        if (Test-Path $ef) {
            $line = (Get-Content $ef) | Where-Object { $_ -match "^SUPABASE_DB_PASSWORD=" }
            if ($line) {
                $env:PGPASSWORD = ($line -split "=", 2)[1].Trim()
                Write-Host "[INFO] Password leida desde .env" -ForegroundColor Cyan
                break
            }
        }
    }
}

if (-not $env:PGPASSWORD) {
    $env:PGPASSWORD = Read-Host "Ingresa la password de Supabase"
}

# -- Verifica que psql este en el PATH --
if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    Write-Host ""
    Write-Host "[ERROR] psql no encontrado." -ForegroundColor Red
    Write-Host "  Instala PostgreSQL y agrega al PATH:" -ForegroundColor Yellow
    Write-Host "  C:\Program Files\PostgreSQL\17\bin" -ForegroundColor Yellow
    Write-Host "  O ejecuta: winget install PostgreSQL.PostgreSQL" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# -- Scripts en orden --
$scripts = @(
    "00_schema_init.sql",
    "01_tabla_usuarios.sql",
    "02_tabla_cuentas_movimientos.sql",
    "03_tabla_creditos.sql",
    "04_tabla_cuotas_credito.sql",
    "05_tabla_gestiones_cobranza.sql",
    "05b_tabla_auditoria.sql",
    "06_datos_calibrados.sql",
    "07_vistas_reportes.sql"
)

Write-Host ""
Write-Host "============================================================" -ForegroundColor Blue
Write-Host "  BancoAndino -- Carga de base de datos Supabase" -ForegroundColor Blue
Write-Host "  Host: $DB_HOST" -ForegroundColor Cyan
Write-Host "  DB:   $DB_NAME  |  User: $DB_USER" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Blue
Write-Host ""

$ok  = 0
$err = 0

foreach ($script in $scripts) {
    $path = Join-Path $PSScriptRoot $script
    if (-not (Test-Path $path)) {
        Write-Host ("  [SKIP] " + $script + " -- no encontrado") -ForegroundColor Yellow
        continue
    }

    Write-Host ("  Ejecutando: " + $script + " ...") -ForegroundColor White -NoNewline

    $out = & psql `
        "--host=$DB_HOST" `
        "--port=$DB_PORT" `
        "--dbname=$DB_NAME" `
        "--username=$DB_USER" `
        "--file=$path" `
        "--set=ON_ERROR_STOP=0" `
        "--quiet" 2>&1

    if ($LASTEXITCODE -eq 0) {
        Write-Host " OK" -ForegroundColor Green
        $ok++
    } else {
        # Ignorar warnings de DROP IF EXISTS (no son errores reales)
        $realErrors = $out | Where-Object { $_ -match "ERROR" -and $_ -notmatch "does not exist" }
        if ($realErrors) {
            Write-Host " ERROR" -ForegroundColor Red
            $realErrors | ForEach-Object { Write-Host ("    " + $_) -ForegroundColor Red }
            $err++
        } else {
            Write-Host " OK" -ForegroundColor Green
            $ok++
        }
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Blue

if ($err -eq 0) {
    Write-Host ("  COMPLETADO: " + $ok + " scripts ejecutados correctamente") -ForegroundColor Green
    Write-Host ""
    Write-Host "  ACCESOS (password: 123456)" -ForegroundColor Cyan
    Write-Host "   Homebanking  -> demo@banco.pe       (CLIENTE)"     -ForegroundColor White
    Write-Host "   Core Asesor  -> asesor@banco.pe     (ASESOR)"      -ForegroundColor White
    Write-Host "   Core Admin   -> admin@banco.pe      (ADMIN)"       -ForegroundColor White
    Write-Host "   Core Jefe    -> jefe@banco.pe       (JEFE_REGIONAL)" -ForegroundColor White
    Write-Host "   Core Riesgos -> riesgos@banco.pe    (RIESGOS)"     -ForegroundColor White
    Write-Host "   Core Comite  -> comite@banco.pe     (COMITE)"      -ForegroundColor White
    Write-Host "   Core Gerencia-> gerencia@banco.pe   (GERENCIA)"    -ForegroundColor White
    Write-Host ""
    Write-Host "  POWER BI -- cadena de conexion:" -ForegroundColor Cyan
    Write-Host ("   Server:   " + $DB_HOST)   -ForegroundColor White
    Write-Host ("   Port:     " + $DB_PORT)   -ForegroundColor White
    Write-Host ("   Database: " + $DB_NAME)   -ForegroundColor White
    Write-Host ("   User:     " + $DB_USER)   -ForegroundColor White
    Write-Host "   SSL:      Required"          -ForegroundColor White
} else {
    Write-Host ("  COMPLETADO CON " + $err + " ERROR(ES). Revisa los mensajes anteriores.") -ForegroundColor Red
}

Write-Host "============================================================" -ForegroundColor Blue
Write-Host ""
