@echo off
rem ─────────────────────────────────────────────────────────────────────────────
rem  Maven Wrapper — BancoConfianza Backend
rem  Usa la distribución de Maven ya descargada en ~/.m2/wrapper/dists
rem ─────────────────────────────────────────────────────────────────────────────

SET MVN_DIST=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\6068d197\bin\mvn.cmd

IF EXIST "%MVN_DIST%" (
    "%MVN_DIST%" %*
) ELSE (
    rem Fallback: buscar cualquier mvn.cmd en las distribuciones del wrapper
    FOR /R "%USERPROFILE%\.m2\wrapper\dists" %%F IN (mvn.cmd) DO (
        "%%F" %*
        GOTO :EOF
    )
    echo.
    echo ERROR: Maven no encontrado.
    echo Instala Maven desde https://maven.apache.org/download.cgi
    echo o agrega mvn al PATH del sistema.
    exit /B 1
)
