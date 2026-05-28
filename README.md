# Inventario / POS API

[![CI](https://github.com/Herrius/inventario-pos-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Herrius/inventario-pos-api/actions/workflows/ci.yml)

API REST de **inventario + POS** en Spring Boot 4 / Java 21 con stock
event-sourced, transacciones de venta atómicas y reportes con índices tuneados.
Incluye frontend mínimo en Vite + React + TS para demo.

**Stack:** Java 21 · Spring Boot 4.0 · Spring Security 7 · Hibernate 7 · PostgreSQL 17 · Flyway · Docker · GitHub Actions

## Qué demuestra este proyecto (vs un CRUD genérico)

1. **Stock como event log append-only.** `StockMovement` es la verdad inmutable
   (entradas, salidas, ajustes, ventas); `Product.stock_actual` es una proyección
   sincronizada en la misma transacción. Permite responder *"¿por qué este SKU
   tiene 12 unidades?"* reproduciendo el log.

2. **Transacciones POS atómicas multi-tabla.** Una venta crea `Sale` + N `SaleItem`
   (con snapshot de precio inmutable) + N `StockMovement` SALIDA + decrementa stock
   con `@Version` anti-sobreventa, todo en la misma unidad atómica. Si CUALQUIER
   paso falla, TODO se revierte.

3. **Reportes con SQL no trivial + índices reales.** Top productos, ventas por
   día/rango, productos bajo mínimo. Índice compuesto `(status, created_at DESC)`
   diseñado y verificado con `EXPLAIN ANALYZE`
   ([docs/REPORTS_QUERY_PLANS.md](docs/REPORTS_QUERY_PLANS.md)).

4. **Test de concurrencia REAL.** 10 cajeros vendiendo la última unidad
   simultáneamente: exactamente 1 venta gana, 9 fallan con 409. Spring Boot
   completo + Postgres real (Testcontainers), sin mocks
   ([SalesConcurrencyTest](src/test/java/com/enrique/inventario/sales/SalesConcurrencyTest.java)).

## Demo

**Credenciales (data sembrada por V8):**
- `admin@demo.com` / `admin123` (ADMIN — configura catálogo, ajustes, reportes)
- `cajero@demo.com` / `cajero123` (CAJERO — solo POS)

**Lo que vas a encontrar al loguearte:** 30 productos, 5 categorías, ~720 ventas
distribuidas en 90 días con variación temporal real (más viernes/sábado, picos
diarios al mediodía).

## Arrancar en local

```bash
# Backend
docker compose up --build
# → http://localhost:8080 · Swagger en /swagger-ui.html · DB en :5432

# Frontend (otro terminal)
cd web && bun install && bun dev
# → http://localhost:5173
```

Verificar salud:
```bash
curl localhost:8080/actuator/health   # {"status":"UP"}
```

## Endpoints

16 endpoints versionados en `/v1/`. Spec completa en `/v3/api-docs`, UI navegable
en `/swagger-ui.html` con botón **Authorize** para JWT.

| Recurso | Endpoints |
|---------|-----------|
| Auth | `POST /v1/auth/register`, `POST /v1/auth/login`, `GET /v1/users/me` |
| Catálogo | `GET/POST/PUT/DELETE /v1/categories`, `GET/POST/PUT/DELETE /v1/products` (paginado + filtros) |
| Inventario | `POST /v1/inventory/entries`, `POST /v1/inventory/adjustments`, `GET /v1/inventory/movements`, `GET /v1/inventory/low-stock` |
| Ventas | `POST /v1/sales`, `GET /v1/sales`, `GET /v1/sales/{id}` |
| Reportes | `GET /v1/reports/sales/daily`, `/sales/range`, `/top-products` |

## Decisiones de diseño (las que defiendo en entrevista)

- **JWT firmado con RS256** (no HS256): la verificación necesita solo la clave
  pública — si algún día separás auth-server y resource-server, no hay que
  compartir secret.
- **`StockMovement` append-only + `Product.stock_actual` como proyección**: la
  trazabilidad del stock vale el costo de mantener dos representaciones.
  Toda mutación de la proyección ocurre en la misma transacción que crea el
  evento, vía un único punto (`StockMovementService.appendMovement`).
- **`@Version` (optimistic locking) en `Product`**: dos cajeros vendiendo el
  último item simultáneamente. El primero gana; el segundo recibe `409
  OPTIMISTIC_LOCK_FAILURE`. Cubierto por `SalesConcurrencyTest`.
- **Snapshot de precio en `SaleItem`**: si mañana cambia el precio de un producto,
  la orden de ayer NO muta. Auditoría.
- **`ApiError` consistente** vía `ResponseEntityExceptionHandler`: TODO error
  (dominio + framework) sale con `errorCode` máquina-legible + `message`
  humano + `requestId`. Un solo formato JSON.
- **CORS configurable**: `app.cors.allowed-origins` con default `*` para demo;
  prod debe ajustar a dominios específicos.

## Pendientes documentados (no bloqueantes)

- **Externalizar claves RSA del JWT**: hoy se generan in-memory al arrancar —
  los tokens no sobreviven reinicios ni se comparten entre instancias. Mover a
  variable `APP_JWT_PRIVATE_KEY` con PEM en base64.
- **Refresh tokens** para sesiones largas.
- **Limitar exposición de Actuator** (`/health` + `/info` son los únicos
  expuestos hoy; añadir rate limiting si se ampliara).
- **Zona horaria del negocio en reportes** (hoy UTC; America/Lima para POS Perú).

## Documentación complementaria

- [CLAUDE.md](CLAUDE.md) — guía para agentes IA al continuar el proyecto.
- [DEPLOY.md](DEPLOY.md) — pasos para Railway / Fly.io.
- [docs/REPORTS_QUERY_PLANS.md](docs/REPORTS_QUERY_PLANS.md) — EXPLAIN ANALYZE
  de los 3 queries de reportes con justificación del índice.
- [web/README.md](web/README.md) — frontend mínimo.
