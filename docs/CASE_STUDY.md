# Case Study — B2 Inventario / POS API

Esta es la historia del proyecto: qué decidí, por qué, qué aprendí, qué haría
distinto. Documento vivo, escrito mientras se construía (no a posteriori).

## Contexto

Segundo proyecto del pivote a desarrollo (post-B1 e-commerce). Construido en
una sesión AI-native con Claude Code. El método: yo defino requisitos y
decisiones de diseño; la IA genera; yo reviso, testeo, itero.

Mercado objetivo: backend developer LATAM (remoto + Perú). Stack más pedido en
Perú = Java + Spring Boot. B1 ya probó CRUD básico; B2 necesita PROFUNDIDAD que
B1 no demuestra.

## El problema de portafolio que B2 resuelve

B1 es sólido pero clónico — auth JWT, catálogo, carrito, órdenes. Un reclutador
que mira ambos repos podría decir *"hizo el mismo proyecto dos veces"*.

B2 se diseñó para demostrar **3 cosas que B1 no demuestra**:

1. Modelado de dominio con event log (no solo entidades CRUD).
2. Transacciones atómicas multi-tabla con concurrencia bajo `@Version`.
3. Reportes con SQL no trivial + diseño de índices verificable con `EXPLAIN ANALYZE`.

Todo lo demás (setup, auth, CRUD básico) se reusó de B1 — pasarlo rápido sin
gastar tiempo de aprendizaje, ese no es el punto.

## Decisiones clave

### 1. `StockMovement` append-only como source of truth

**El dilema:** dos opciones para el stock.
- A) `Product.stock` mutable, `UPDATE` directo en cada venta/entrada.
- B) `StockMovement` append-only como log; `Product.stock_actual` es proyección.

**Elegí B** sabiendo que cuesta más código y mantenimiento. Razones:
- Un POS sin trazabilidad es responsable de discrepancias contables imposibles
  de auditar.
- En entrevista, "¿cómo sabés por qué este SKU tiene 12 unidades?" se responde
  reproduciendo el log. Defensible vs respuesta "porque la última venta lo dejó así".
- Habilita el reporte de mermas/ajustes sin tablas adicionales (filtro por tipo).

**El precio que pagas:** doble representación. Mitigado con:
- Un único punto de mutación: `StockMovementService.appendMovement`. Si alguien
  modifica `Product.stockActual` desde otro service, el invariante se rompe.
  Mantener este punto único es disciplina, no garantía técnica.
- Comentarios explícitos en `Product.aplicarDeltaStock` y en la migración V5.

### 2. JPA constructor expression vs proyecciones planas

Intenté usar `new com.example.DTO(...)` en JPQL para el reporte daily summary.
Hibernate 7 falló al hacer match con `long`/`Long` del Java record. Diagnóstico:
no encuentra constructor compatible cuando una columna SUM/AVG llega como
BigDecimal pero el record la espera tipada estricta.

**Decisión:** usar proyección plana (interface) con queries nativas para los 3
reportes. Pierdo type-safety de la JPQL pero gano predictibilidad. En un proyecto
más grande consideraría jOOQ para SQL fuertemente tipado.

### 3. Sale.items con cascade=ALL

Considerar:
- Persistir Sale + items en transacciones separadas (más control, más código).
- Persistir Sale con cascade=ALL y orphanRemoval=true (menos código).

Elegí cascade. Razón: el ciclo de vida de un SaleItem está estrictamente atado
a su Sale (no existen items huérfanos). Hibernate maneja la persistencia en
orden correcto (Sale primero → genera ID → items con FK).

**El gotcha que descubrí:** `saleRepository.save(sale)` no garantiza que el ID
esté poblado hasta el flush. Agregué `saleRepository.flush()` explícito antes
de pasar `sale.getId()` a `StockMovementService.appendMovement`. Sin esto, los
movements quedaban con `saleId=null`.

### 4. CORS allowedOriginPatterns vs allowedOrigins

Para que `allowCredentials=true` funcione con wildcard `*`, hay que usar
`allowedOriginPatterns(["*"])` en lugar de `allowedOrigins(["*"])`. Spring tira
una excepción explícita si combinas mal. Documentado en `CorsConfig`.

### 5. El parámetro `:q` y "lower(bytea)"

Postgres falló con `function lower(bytea) does not exist` cuando un query JPQL
pasaba `:q = NULL` y combinaba con `LOWER(...)`. Causa: Hibernate hace
`setObject(null)` sin tipo SQL declarado; Postgres deduce `bytea`.

**Fix:** comparar contra string vacío (`:q = ''`) en el SQL y normalizar en el
service (`q == null ? "" : q`). Documentado en el Javadoc del repo.

## El test de concurrencia (la pieza de portafolio)

`SalesConcurrencyTest` levanta Spring Boot completo + Postgres real
(Testcontainers) y dispara 10 POST `/v1/sales` simultáneos contra un producto
con stock=1. Usa `CountDownLatch` para que los 10 threads disparen al mismo
instante (máxima contención).

Salida real en CI:
```
[concurrency] statuses=[409, 409, 409, 409, 409, 409, 409, 201, 409, 409]
Tests run: 1, Failures: 0
```

Exactamente 1 ganador. Sin esto, sobreventa.

## Lo que NO hice (y por qué)

- **Microservicios.** Un POS para MYPE no necesita 5 servicios. YAGNI.
- **CQRS / Event Sourcing completo.** Hay un toque (StockMovement) pero no
  Event Store; sería sobre-ingeniería.
- **Cache Redis para reportes.** Los reportes tardan <0.1ms con índices. No hay
  problema de latencia que justifique cache.
- **Refresh tokens.** Está documentado como pendiente; agregar cost-benefit pobre
  para un demo.
- **i18n.** El negocio es Perú; mensajes en español es lo correcto.
- **Tests E2E del frontend.** F1 los tendrá. B2 es backend-first.

## Lo que haría distinto

- **Empezaría con el OpenAPI ANTES.** En B1 lo diferí; en B2 lo metí en M1 y
  ayudó muchísimo a iterar. La próxima vez, primer commit.
- **Más tests integrados desde M2.** En B2 los tests llegaron en M6. Sumar 1-2
  por slice habría detectado el bug del `:q` antes.
- **CI con coverage report** desde el día 1. No lo metí; F1 lo tendrá.
- **Profile dev vs prod en application.yml.** Hoy hay una sola config; en F1
  separar.

## Qué aprendí

- Spring Security 7 cambió DSL: `authorizeHttpRequests` con lambda. No tiene API
  fluent vieja.
- Spring Boot 4 todavía no tiene todas las dependencias 100% compatibles
  (springdoc 2.8.x funciona, no probé otros) — verificar primero.
- Optimistic locking en Hibernate es transparente pero el manejo del error
  (`OptimisticLockingFailureException` → 409) hay que cablearlo a mano.
- Postgres + JPQL con parámetros nullable requiere cuidado (cast, comparar a
  vacío, o pasar siempre tipo declarado).

## Métricas finales

- 23 commits desde M0 a M7, CI verde todo el camino.
- 5 tests verdes en CI (1 contextLoads + 3 unit + 1 concurrencia HTTP real).
- 16 endpoints documentados en OpenAPI/Swagger.
- 8 migraciones Flyway.
- 30 productos, ~720 ventas seed (cobertura realista de reportes en demo).
- Frontend: 5 pantallas, ~500 líneas TS, 76 KB gzip.
