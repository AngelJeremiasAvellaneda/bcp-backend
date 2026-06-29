# BancoConfianza — Backend Spring Boot

API REST con Spring Boot 3.3, Spring Security (JWT), Spring Data JPA y PostgreSQL (Supabase).

## Requisitos

- Java 21+
- Maven 3.9+ (o usa el wrapper `./mvnw`)
- Cuenta en [Supabase](https://supabase.com) con un proyecto PostgreSQL

## Configuración

1. Copia el archivo de ejemplo y completa tus credenciales:

```bash
cp .env.example .env
```

Edita `.env`:

```env
SUPABASE_DB_URL=jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=tu_password_supabase
JWT_SECRET=cualquier_texto_secreto_largo
```

2. Las variables del `.env` se leen automáticamente si usas el plugin de Spring Boot DevTools o las exportas en tu terminal:

```bash
# Windows PowerShell
Get-Content .env | ForEach-Object {
  if ($_ -match '^([^#][^=]+)=(.+)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
  }
}

# Linux / macOS
export $(grep -v '^#' .env | xargs)
```

## Levantar el servidor

Maven no necesita estar instalado globalmente. Usa el wrapper incluido:

```cmd
# Windows (PowerShell o CMD) — desde la carpeta backend\
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
./mvnw spring-boot:run
```

El servidor arranca en `http://localhost:8080`.

## Endpoints principales

| Método | Ruta                              | Auth | Descripción                        |
|--------|-----------------------------------|------|------------------------------------|
| POST   | `/api/auth/register`              | No   | Registrar nuevo usuario            |
| POST   | `/api/auth/login`                 | No   | Login → devuelve JWT               |
| GET    | `/api/cuentas`                    | JWT  | Cuentas del usuario autenticado    |
| GET    | `/api/cuentas/{id}/movimientos`   | JWT  | Últimos 10 movimientos             |
| POST   | `/api/cuentas/transferir`         | JWT  | Transferencia entre cuentas        |
| POST   | `/api/cuentas/prueba`             | JWT  | Crear cuenta de prueba (dev only)  |

## Flujo de autenticación

```
POST /api/auth/login
Body: { "email": "user@example.com", "password": "123456" }

Response: {
  "token": "eyJhbGci...",
  "user": { "id": 1, "name": "Juan Pérez", "email": "user@example.com" }
}
```

El token se envía en el header de las peticiones protegidas:
```
Authorization: Bearer eyJhbGci...
```

## Estructura del proyecto

```
src/main/java/pe/bancoconfianza/backend/
├── config/          # SecurityConfig, JwtProperties, CorsProperties
├── controller/      # AuthController, CuentaController
├── dto/             # LoginRequest, RegisterRequest, AuthResponse, CuentaDto, MovimientoDto, TransferenciaRequest
├── exception/       # GlobalExceptionHandler
├── model/           # Usuario, Cuenta, Movimiento
├── repository/      # UsuarioRepository, CuentaRepository, MovimientoRepository
├── security/        # JwtService, JwtAuthFilter
└── service/         # AuthService, CuentaService, UsuarioService
```

## Tablas en Supabase

Hibernate crea las tablas automáticamente con `spring.jpa.hibernate.ddl-auto=update`.
Las tablas generadas son: `usuarios`, `cuentas`, `movimientos`.
