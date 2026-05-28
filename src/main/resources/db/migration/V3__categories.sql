-- M2: categorías. Agrupación simple para clasificar productos del catálogo.
-- nombre UNIQUE: dos categorías no pueden tener el mismo nombre (regla de negocio).
CREATE TABLE categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
