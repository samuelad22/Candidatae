-- =====================================================================
--  V3 — Email Synchronization Columns (IMAP / OAuth2).
--
--  All new columns follow the English naming convention agreed by the team:
--   - last_sync_at: timestamp of the last successful synchronization.
--   - uid_validity: IMAP mailbox UIDVALIDITY value to detect re-indexing.
--   - last_uid:     highest IMAP message UID processed so far.
--   - sync_status:  synchronization state (IDLE, SYNCING, SUCCESS, ERROR).
-- =====================================================================

ALTER TABLE cuenta_correo
    ADD COLUMN last_sync_at TIMESTAMPTZ,
    ADD COLUMN uid_validity BIGINT,
    ADD COLUMN last_uid     BIGINT,
    ADD COLUMN sync_status  VARCHAR(50) NOT NULL DEFAULT 'IDLE';

CREATE INDEX idx_cuenta_correo_last_sync ON cuenta_correo (usuario_id, last_sync_at);

