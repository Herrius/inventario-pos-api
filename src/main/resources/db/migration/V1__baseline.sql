-- V1 baseline: establece el pipeline de migraciones Flyway.
-- Es intencionalmente un no-op para verificar, ya en M0, que Flyway corre y
-- registra en flyway_schema_history ANTES de añadir tablas reales.
-- El esquema de dominio (usuarios, productos, órdenes) empieza en M1+.
SELECT 1;
