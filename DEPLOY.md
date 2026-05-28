# Despliegue — Railway (recomendado)

Railway detecta el `Dockerfile` automáticamente, provisiona PostgreSQL gestionado
e inyecta las variables `PG*` y `PORT` que la app ya lee (ver `application.yml`).

## Pasos

### 1. Crear proyecto + servicio Postgres
1. Login en [railway.app](https://railway.app) (con cuenta de GitHub).
2. **New Project** → **Deploy from GitHub repo** → seleccionar `Herrius/inventario-pos-api`.
3. Railway construye la imagen del `Dockerfile` y la despliega como **servicio app**.
4. En el mismo proyecto → **New** → **Database** → **Add PostgreSQL**.

### 2. Conectar app ↔ Postgres
En el servicio app → **Variables** → **Add Reference Variable**. Agregar (referencias
al servicio Postgres, no valores hardcodeados):

| Variable | Valor (referencia) |
|----------|---------------------|
| `PGHOST` | `${{ Postgres.PGHOST }}` |
| `PGPORT` | `${{ Postgres.PGPORT }}` |
| `PGDATABASE` | `${{ Postgres.PGDATABASE }}` |
| `PGUSER` | `${{ Postgres.PGUSER }}` |
| `PGPASSWORD` | `${{ Postgres.PGPASSWORD }}` |

Railway expone esas variables desde su servicio Postgres; la app las lee con su
fallback `PG*` (ver `application.yml`).

### 3. (Opcional) Configurar JWT en producción
Hoy las claves RSA del JWT se generan **in-memory** al arrancar — los tokens no
sobreviven reinicios ni se comparten entre instancias. Para producción real
(M7 hardening), externalizar la clave a una variable `APP_JWT_PRIVATE_KEY` con el
PEM en base64.

### 4. Generar dominio público
Servicio app → **Settings** → **Networking** → **Generate Domain**. Railway crea
una URL `*.up.railway.app`. Verificar:

```bash
curl https://<tu-dominio>.up.railway.app/actuator/health
# {"status":"UP"}
```

### 5. Bootstrap del primer admin
El registro crea usuarios con rol `CAJERO` por defecto. Para tener un ADMIN,
registrar un usuario y promoverlo en la base. En Railway → servicio Postgres →
**Data** → ejecutar:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'tu@correo.com';
```

## Alternativas

- **Fly.io**: similar, requiere `fly launch` + `fly secrets set DB_URL=…`. Más control,
  más setup. Para Postgres usa su `flyctl postgres create`.
- **Render**: igual de simple que Railway, free tier con sleep tras inactividad
  (mala UX para portafolio).
- **VPS propio** (DigitalOcean / Hetzner): `docker compose up -d` directo + nginx-proxy
  + certbot. Más senior, más mantenimiento.

Railway es la elección por defecto para portafolio: cero sleep, URL pública estable
y Postgres gestionado en el free tier.
