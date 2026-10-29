package com.candidatae.integration.imap;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.ProveedorCorreo;
import com.candidatae.dto.EmailSyncDto;

import java.time.Instant;

/**
 * Strategy interface for reading emails from external mailbox providers
 * (e.g., Gmail via IMAP, Outlook via Microsoft Graph API).
 */
public interface EmailReaderProvider {

    /**
     * Returns whether this reader supports the given email provider.
     */
    boolean supports(ProveedorCorreo provider);

    /**
     * Validates that the given email and credential (e.g., App Password or OAuth token)
     * can authenticate against the remote mailbox server.
     */
    void validateCredentials(String email, String rawCredential);

    /**
     * Fetches new emails incrementally since the account's last UID or fallback timestamp.
     *
     * @param account the email account containing encrypted credentials and sync cursors.
     * @param since   fallback start timestamp when no valid UID cursor exists.
     * @return batch result with updated UID cursor and matching job-related emails.
     */
    EmailSyncDto.FetchBatchResult fetchNewEmails(CuentaCorreo account, Instant since);
}

