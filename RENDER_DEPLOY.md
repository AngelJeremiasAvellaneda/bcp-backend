# 🚀 Deploy Backend a Render

## Paso 1: Preparar Render

1. Ve a https://render.com
2. Sign up o login
3. Dashboard → New+ → Web Service

---

## Paso 2: Conectar Repositorio GitHub

1. Click en **GitHub** (Connect your repository)
2. Autoriza Render a acceder a tu GitHub
3. Selecciona el repositorio: `banco`
4. Selecciona **Root directory**: `backend`

---

## Paso 3: Configurar Servicio

### Build Settings:
- **Name**: `banco-confianza-api`
- **Runtime**: Java
- **Runtime Version**: 21
- **Build Command**: `mvn clean package -DskipTests`
- **Start Command**: `java -jar target/homebanking-backend-1.0.0.jar`

### Instance:
- **Starter** ($7/mes) — Para testing/staging
- **Standard** ($12/mes) — Para producción

---

## Paso 4: Configurar Variables de Entorno

En Render Dashboard → Environment:

```
SUPABASE_DB_URL=jdbc:postgresql://aws-1-us-east-2.supabase.com:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres.wxqqyitfomndkgtgorow
SUPABASE_DB_PASSWORD=<tu_password>
SUPABASE_URL=https://wxqqyitfomndkgtgorow.supabase.co
JWT_SECRET=<genera_aleatorio_base64>
JWT_EXPIRATION_MS=86400000
CORS_ORIGINS=https://homebanking.bancoconfianza.pe,https://core.bancoconfianza.pe
LOGGING_LEVEL_PE_BANCOCONFIANZA=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=WARN
```

⚠️ **IMPORTANTE**: 
- JWT_SECRET debe ser diferente en producción
- Para generar: `openssl rand -base64 32`
- Database password desde Supabase dashboard

---

## Paso 5: Deploy

1. Click **Create Web Service**
2. Render auto-deploya cuando haces push a `main`
3. Ver logs en Render Dashboard → Logs

---

## Paso 6: Verificar Deployment

Una vez deployment complete, deberías ver:

```
✅ Service created
✅ Build started
✅ Build completed
✅ Service live at: https://banco-confianza-api.onrender.com
```

Test la API:

```bash
curl https://banco-confianza-api.onrender.com/api/public/health

# Response:
# {"status":"UP","service":"BancoConfianza API"}
```

---

## Auto-Deploy (Recomendado)

Render auto-deploya en cada push a `main`. Para deshabilitar:

Dashboard → Settings → Auto-Deploy → Disable

---

## Logs & Debugging

```bash
# Ver logs en tiempo real
# Render Dashboard → Logs

# O usar Render CLI:
render logs banco-confianza-api
```

---

## Troubleshooting

### Build falla:
1. Verificar Java 21 disponible
2. Checkear pom.xml válido
3. Ver logs de build en Render

### Connection timeout a Supabase:
1. Verificar `SUPABASE_DB_URL` es correcto
2. Checkear Supabase network access (sin IP whitelist)
3. Verificar `sslmode=require` está en URL

### 502 Bad Gateway:
1. Checkear logs en Render
2. Verificar `JWT_SECRET` configurado
3. Verificar database variables correctas

### Port issues:
Render asigna puerto automáticamente. Spring Boot debería escuchar en puerto assignado:
- `server.port=$PORT` (automático en Render)
- O dejar default 8080 (Render lo redirige)

---

## Dominio Personalizado

1. Render Dashboard → Settings → Custom Domain
2. Agregar: `api.bancoconfianza.pe`
3. Configurar DNS:
   ```
   CNAME: api.bancoconfianza.pe → banco-confianza-api.onrender.com
   ```

---

## Monitoreo

Render proporciona:
- ✅ Logs en vivo
- ✅ Métricas de CPU/Memory
- ✅ Auto-scaling (planes pro)
- ✅ SSL/TLS automático

---

## Costo Estimado

- **Starter**: $7/mes (perfecto para testing)
- **Standard**: $12/mes (producción)
- **Premium**: $25/mes (high traffic)

---

## Pull Request: Deploy Preview (Opcional)

Para auto-deploy PRs a ambiente de staging:

1. Render Dashboard → Settings
2. Enable **Deploy preview for pull requests**
3. Cada PR deployará a URL temporal

---

## Rollback

Si deployment falla:
1. Render automáticamente mantiene versión anterior
2. Dashboard → Deployments → Select previous
3. Click **Rollback**

---

## Environment Variables Seguras

Para cambiar `JWT_SECRET` sin redeploy:

1. Render Dashboard → Environment
2. Edit variable
3. Redeploy automático

---

**Próximo paso**: Deploy frontends a Vercel (ver `../frontend/README.md`)
