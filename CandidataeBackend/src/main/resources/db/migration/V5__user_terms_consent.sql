-- =====================================================================
--  V5 — Registro de consentimiento de Términos y Condiciones (RGPD / LOPDGDD).
--
--  Permite auditar el cumplimiento del Principio de Responsabilidad Proactiva
--  (Accountability, Art. 5.2 RGPD):
--   - terms_accepted: booleano que indica si el usuario ha aceptado los términos.
--   - terms_accepted_at: marca temporal con zona horaria del momento de aceptación.
--   - terms_version: versión legal específica aceptada (ej. '1.0').
-- =====================================================================

ALTER TABLE users
    ADD COLUMN terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN terms_accepted_at TIMESTAMPTZ,
    ADD COLUMN terms_version VARCHAR(20);

