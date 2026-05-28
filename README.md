# Inventario / POS API

[![CI](https://github.com/Herrius/inventario-pos-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Herrius/inventario-pos-api/actions/workflows/ci.yml)

API REST de **inventario + POS** en Spring Boot. Pieza de portafolio del pivote a
desarrollo de Enrique. Stack: **Java 21 · Spring Boot 4 · PostgreSQL · Docker**.

## Qué demuestra este proyecto

A diferencia de un CRUD genérico:

- **Stock como event log append-only.** `StockMovement` es la verdad inmutable
  (entradas, salidas, ajustes); `Product.stock_actual` es una proyección. Permite
  responder *"¿por qué este SKU tiene 12 unidades?"* reproduciendo el log.
- **Transacciones POS atómicas multi-tabla.** Una venta crea `Sale` + N `SaleItem`
  (snapshot de precio) + N `StockMovement` SALIDA + decrementa stock con `@Version`
  anti-sobreventa, todo en la misma transacción.
- **Reportes con SQL real.** Top productos, ventas por día/rango, productos bajo
  mínimo — con índices diseñados y `EXPLAIN ANALYZE` documentado.

## Requisitos
Docker + Docker Compose (no necesitas Java/Maven en el host).

## Arrancar
```bash
docker compose up --build
```
App en http://localhost:8080 · base en `localhost:5432`.

Verificar salud:
```bash
curl localhost:8080/actuator/health
# {"status":"UP"}
```

Apagar y limpiar (borra datos):
```bash
docker compose down -v
```

## Desarrollo (Dev Container)
Abrir la carpeta en VS Code → **Reopen in Container**. Levanta un entorno con
Java 21 + Maven y un Postgres, sin instalar nada en tu máquina. Dentro:
```bash
./mvnw spring-boot:run   # corre la app
./mvnw test              # tests (Testcontainers)
```

## Estado

En construcción. Roadmap en `CLAUDE.md`.

## Documentación
Bitácora detallada en Obsidian: *"Proyecto B2 — Inventario / POS (Spring Boot)"*.
