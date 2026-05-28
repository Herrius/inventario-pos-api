# B2 Inventario / POS API — Guía para agentes

> Este archivo es la fuente de verdad. `AGENTS.md` es un symlink a él (estándar
> neutral leído por Cursor/Codex/etc.); `CLAUDE.md` lo lee Claude Code. Editar solo este.

Proyecto del pivote a desarrollo de Enrique (track Backend, segunda pieza tras B1).
API REST de **inventario + POS** con stock event-sourced, transacciones de venta
atómicas y reportes con índices tuneados. Construido **AI-native** siguiendo la
skill `desarrollo-backend` (leerla al trabajar aquí).

## Diferenciadores vs B1 (lo que B2 demuestra y B1 no)
1. **Stock como event log append-only** (`StockMovement` es la verdad,
   `Product.stock_actual` es proyección/cache).
2. **Transacciones POS atómicas multi-tabla** (venta = `Sale` + `SaleItem` +
   N `StockMovement` SALIDA + decremento de stock con `@Version`).
3. **Reportes con SQL no trivial + índices + `EXPLAIN ANALYZE`** (top productos,
   ventas por día, agregaciones temporales).

## Stack
- Java 21 · Spring Boot 4.0.x · Maven (wrapper `./mvnw`)
- PostgreSQL 17 · Flyway (migraciones) · Spring Data JPA
- Tests: JUnit 5 + Mockito + Testcontainers
- Docker (compose: db + app) · Dev Container para editar
- Frontend (M7): Vite + React + TypeScript en `web/` — mínimo, demo-only

## Convenciones (seguir SIEMPRE)
- Arquitectura en capas: controller → service → repository.
- DTOs (`record`) separados de entidades JPA. Nunca exponer entidades en la API.
- Inyección por constructor.
- Validación con Bean Validation en los DTOs de entrada.
- Errores: formato JSON único vía `@RestControllerAdvice` (error_code + message + request_id).
- Flyway es el dueño del esquema (`ddl-auto: validate`). Nunca editar una migración aplicada; crear una nueva.
- Tests como parte del DoD, no opcionales.
- Commits chicos con el porqué.

## Comandos
- Levantar todo en Docker:      `docker compose up --build`
- Solo la base de datos:        `docker compose up -d db`
- Correr la app (en dev):       `./mvnw spring-boot:run`
- Tests:                        `./mvnw test`   (requiere Docker para Testcontainers)
- Health:                       `curl localhost:8080/actuator/health`

## Verificación de endpoints
El hook PAI bloquea `curl POST` con cuerpo (lo confunde con exfiltración). Usar
**Python `urllib`** para POSTs (login, crear recursos, mandar `Authorization: Bearer`).

## Roles
- `ADMIN` — configura catálogo, registra entradas de stock, ve todo, hace ajustes.
- `CAJERO` — registra ventas y ve productos. NO modifica catálogo.

## Roadmap (hitos)
- [ ] M0 Setup dockerizado (app + Postgres + Flyway + /health)
- [ ] M0.5 Despliegue público (Railway/Fly.io) — URL viva
- [ ] M1 Auth JWT RS256 + roles ADMIN/CAJERO + `ApiError` + OpenAPI/Swagger
- [ ] M2 Catálogo (Category + Product)
- [ ] M3 StockMovement append-only + entries + log
- [ ] M4 POST /sales (transacción atómica)
- [ ] M5 Reportes (índices + EXPLAIN ANALYZE)
- [ ] M5.5 Flyway seed con ~3 meses de datos realistas
- [ ] M6 Tests + test de concurrencia REAL (10 ventas paralelas → 1 gana, 9 fallan)
- [ ] M7 Frontend mínimo (5 pantallas) + README de portafolio + case study

## No hacer
- No meter Swagger/springdoc sin verificar primero compat con Spring Boot 4.
- No usar `Product.stock_actual` como fuente de verdad; siempre pasar por `StockMovement`.
- No mutar `StockMovement` (append-only); para corregir un error, crear otro movimiento de tipo `AJUSTE`.
- No microservicios, colas ni abstracciones que el MVP no necesita.
- No secretos en el código ni en el repo (`.env` no commiteado).
