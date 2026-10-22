-- =====================================================================
--  V1 — Esquema inicial.
--
--  Este script crea el esquema base de la aplicación. A partir de aquí
--  Flyway es la ÚNICA fuente de verdad del esquema: nunca más hay que
--  tocar el DDL a mano ni usar spring.jpa.hibernate.ddl-auto=update.
--
--  IMPORTANTE: la tabla de usuarios se llama "users" (y no "usuario") porque
--  "user" es una palabra reservada en PostgreSQL. El resto del proyecto usa
--  "users" de forma consistente.
-- =====================================================================

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    name          VARCHAR(100),
    auth_provider VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    created_at    TIMESTAMP    NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- Índice adicional por email para las consultas de login (findByEmail),
-- que se ejecutan en cada petición autenticada.
CREATE INDEX idx_users_email ON users (email);
