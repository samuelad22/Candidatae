-- =====================================================================
--  V3 — Esquema para el módulo de Candidaturas.
--
--  Tablas para la gestión y seguimiento de candidaturas de empleo:
--   - plataforma: catálogo de portales (LinkedIn, InfoJobs, Indeed, etc.)
--   - empresa: catálogo normalizado de empresas para deduplicación.
--   - candidatura: entidad central de la solicitud de empleo.
--   - historial_estado: registro de transiciones de estado (para auditoría y embudo).
--   - contacto: reclutadores asociados a una candidatura.
--   - nota: anotaciones del usuario sobre una candidatura.
--
--  Aislamiento multiusuario:
--   - "usuario_id" en "candidatura" referencia a "users(id)" ON DELETE CASCADE.
--   - "contacto", "nota" e "historial_estado" se vinculan a "candidatura" ON DELETE CASCADE.
-- =====================================================================

-- 1. Catálogo de plataformas de empleo
CREATE TABLE IF NOT EXISTS plataforma (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo               VARCHAR(50)  NOT NULL,
    nombre               VARCHAR(100) NOT NULL,
    plataforma_origen_id BIGINT REFERENCES plataforma (id) ON DELETE SET NULL,
    CONSTRAINT uk_plataforma_codigo UNIQUE (codigo)
);

-- 2. Catálogo de empresas (con nombre normalizado para deduplicación)
CREATE TABLE IF NOT EXISTS empresa (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre             VARCHAR(255) NOT NULL,
    nombre_normalizado VARCHAR(255) NOT NULL,
    CONSTRAINT uk_empresa_nombre_normalizado UNIQUE (nombre_normalizado)
);

CREATE INDEX IF NOT EXISTS idx_empresa_normalizado ON empresa (nombre_normalizado);

-- 3. Entidad central: candidatura
CREATE TABLE IF NOT EXISTS candidatura (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    empresa_id          BIGINT NOT NULL REFERENCES empresa (id),
    plataforma_id       BIGINT REFERENCES plataforma (id) ON DELETE SET NULL,
    puesto              VARCHAR(255) NOT NULL,
    ubicacion           VARCHAR(255),
    modalidad           VARCHAR(50),
    salario_texto       VARCHAR(100),
    enlace_oferta       TEXT,
    fecha_solicitud     DATE NOT NULL,
    estado_actual       VARCHAR(50) NOT NULL,
    fecha_ultimo_cambio TIMESTAMPTZ NOT NULL,
    confianza           NUMERIC(3, 2),
    clave_deduplicacion VARCHAR(255),
    fusionada_en_id     BIGINT REFERENCES candidatura (id) ON DELETE SET NULL
);

-- Índices críticos para rendimiento y aislamiento de datos:
CREATE INDEX IF NOT EXISTS idx_candidatura_usuario ON candidatura (usuario_id);
CREATE INDEX IF NOT EXISTS idx_candidatura_usuario_estado ON candidatura (usuario_id, estado_actual);
CREATE INDEX IF NOT EXISTS idx_candidatura_dedup ON candidatura (clave_deduplicacion);
CREATE INDEX IF NOT EXISTS idx_candidatura_empresa ON candidatura (empresa_id);
CREATE INDEX IF NOT EXISTS idx_candidatura_plataforma ON candidatura (plataforma_id);

-- 4. Historial de estados (base del embudo y trazabilidad)
CREATE TABLE IF NOT EXISTS historial_estado (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidatura_id      BIGINT NOT NULL REFERENCES candidatura (id) ON DELETE CASCADE,
    estado_anterior     VARCHAR(50),
    estado_nuevo        VARCHAR(50) NOT NULL,
    fecha_evento        TIMESTAMPTZ NOT NULL,
    origen              VARCHAR(20) NOT NULL, -- 'AUTOMATICO' o 'MANUAL'
    correo_procesado_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_historial_candidatura ON historial_estado (candidatura_id);
CREATE INDEX IF NOT EXISTS idx_historial_candidatura_fecha ON historial_estado (candidatura_id, fecha_evento);

-- 5. Contactos asociados a la candidatura (reclutadores, entrevistadores)
CREATE TABLE IF NOT EXISTS contacto (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidatura_id BIGINT NOT NULL REFERENCES candidatura (id) ON DELETE CASCADE,
    nombre         VARCHAR(150),
    email          VARCHAR(255),
    rol            VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_contacto_candidatura ON contacto (candidatura_id);

-- 6. Notas de seguimiento del usuario
CREATE TABLE IF NOT EXISTS nota (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidatura_id BIGINT NOT NULL REFERENCES candidatura (id) ON DELETE CASCADE,
    texto          TEXT NOT NULL,
    fecha          TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_nota_candidatura ON nota (candidatura_id);

-- =====================================================================
--  Datos Iniciales — Catálogo de Plataformas (Sprint 1)
-- =====================================================================

-- Inserción de plataformas independientes
INSERT INTO plataforma (codigo, nombre, plataforma_origen_id)
VALUES 
    ('LINKEDIN', 'LinkedIn', NULL),
    ('INFOJOBS', 'InfoJobs', NULL),
    ('INDEED', 'Indeed', NULL),
    ('TECNOEMPLEO', 'Tecnoempleo', NULL)
ON CONFLICT (codigo) DO NOTHING;

-- Inserción de Glassdoor vinculada a Indeed como plataforma_origen_id
INSERT INTO plataforma (codigo, nombre, plataforma_origen_id)
SELECT 'GLASSDOOR', 'Glassdoor', id 
FROM plataforma 
WHERE codigo = 'INDEED'
ON CONFLICT (codigo) DO NOTHING;
