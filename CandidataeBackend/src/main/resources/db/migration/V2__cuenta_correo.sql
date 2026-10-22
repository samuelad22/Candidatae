-- =====================================================================
--  V2 — Cuentas de correo vinculadas por el usuario.
--
--  Una fila = una bandeja de entrada (hoy solo Gmail) que el usuario ha
--  conectado a Candidatae para que el backend lea sus notificaciones.
--
--  Decisiones de diseño (ver docs/PLAN-CRUD-CUENTA-CORREO.md):
--   - "usuario_id" referencia "users(id)" porque esa es la tabla real
--     ("user" es palabra reservada en PostgreSQL).
--   - ON DELETE CASCADE: si se borra el usuario, sus cuentas de correo
--     (y sus tokens) desaparecen con él.
--   - "proveedor" se añadió porque aparece en la documentación del
--     proyecto aunque faltaba en el CREATE TABLE inicial.
--   - "fecha_desconexion" permite auditar la desconexión sin perder el
--     histórico: el DELETE marca DESCONECTADA en vez de borrar la fila.
--   - UNIQUE (usuario_id, email_cuenta): la misma bandeja no se vincula
--     dos veces por el mismo usuario.
-- =====================================================================

CREATE TABLE cuenta_correo (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id             BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    proveedor              VARCHAR(20)  NOT NULL DEFAULT 'GMAIL',
    email_cuenta           VARCHAR(255) NOT NULL,
    refresh_token_cifrado  BYTEA,
    cursor_sincronizacion  VARCHAR(255),
    estado_conexion        VARCHAR(50)  NOT NULL DEFAULT 'PENDIENTE',
    fecha_conexion         TIMESTAMPTZ,
    fecha_desconexion      TIMESTAMPTZ,
    CONSTRAINT uk_cuenta_correo_usuario_email UNIQUE (usuario_id, email_cuenta)
);

-- El 100 % de las consultas filtra por usuario_id (aislamiento de datos),
-- así que este índice es crítico.
CREATE INDEX idx_cuenta_correo_usuario ON cuenta_correo (usuario_id);

-- Índice compuesto para el scheduler: buscar las cuentas activas de un usuario.
CREATE INDEX idx_cuenta_correo_usuario_estado ON cuenta_correo (usuario_id, estado_conexion);
