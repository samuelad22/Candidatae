package com.candidatae.dto;

import com.candidatae.domain.ProveedorCorreo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Objects for email synchronization and IMAP account connection.
 */
public final class EmailSyncDto {

    private EmailSyncDto() {
    }

    /**
     * Request payload to connect an email account via IMAP using an App Password.
     */
    public record ConnectImapRequest(
            @NotBlank(message = "El email de la cuenta es obligatorio")
            @Email(message = "El email de la cuenta no tiene un formato válido")
            @Size(max = 255, message = "El email de la cuenta no puede superar los 255 caracteres")
            String email,

            @NotBlank(message = "La contraseña de aplicación es obligatoria")
            @Size(min = 8, max = 128, message = "La contraseña de aplicación debe tener entre 8 y 128 caracteres")
            String appPassword,

            ProveedorCorreo provider
    ) {
    }

    /**
     * Transport representation of a raw email retrieved from an external provider.
     */
    public record RawEmailDto(
            Long uid,
            String messageId,
            String fromAddress,
            String subject,
            Instant receivedAt,
            String plainTextBody,
            String htmlBody
    ) {
    }

    /**
     * Result of an incremental batch fetch from a mailbox provider.
     */
    public record FetchBatchResult(
            Long uidValidity,
            Long highestUid,
            int emailsScanned,
            List<RawEmailDto> matchedEmails
    ) {
    }

    /**
     * Summary of a job-related email detected during synchronization.
     */
    public record DetectedEmailSummaryDto(
            Long uid,
            String fromAddress,
            String subject,
            Instant receivedAt,
            String detectedPlatform,
            String detectedStatus
    ) {
    }

    /**
     * Summary produced by the candidacy extraction service.
     */
    public record ExtractionSummaryDto(
            int candidaciesCreated,
            int candidaciesUpdated,
            List<DetectedEmailSummaryDto> detectedEmails
    ) {
    }

    /**
     * Public API response for an email synchronization run.
     */
    public record SyncResultDto(
            Long accountId,
            int emailsScanned,
            int candidaciesCreated,
            int candidaciesUpdated,
            Instant lastSyncAt,
            String syncStatus,
            List<DetectedEmailSummaryDto> detectedEmails
    ) {
    }

    /**
     * Aggregate synchronization summary across multiple email accounts belonging to a user.
     */
    public record MultiAccountSyncResultDto(
            int accountsSynced,
            int totalEmailsScanned,
            int totalCandidaciesCreated,
            int totalCandidaciesUpdated,
            Instant syncedAt,
            List<SyncResultDto> accountResults
    ) {
    }
}

