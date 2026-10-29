package com.candidatae.service;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.EstadoConexion;
import com.candidatae.domain.ProveedorCorreo;
import com.candidatae.domain.SyncStatus;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CuentaCorreoDto;
import com.candidatae.dto.EmailSyncDto;
import com.candidatae.exception.ApiException;
import com.candidatae.integration.imap.EmailReaderProvider;
import com.candidatae.integration.imap.GmailImapReader;
import com.candidatae.repository.CuentaCorreoRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.TokenCipherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Orchestrates email account synchronization (initial 7-day window and UID-based
 * incremental sync), first-daily-login triggers, and IMAP App Password connections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSyncService {

    private final CuentaCorreoRepository cuentaCorreoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenCipherService tokenCipherService;
    private final List<EmailReaderProvider> emailReaderProviders;
    private final CandidacyExtractionService candidacyExtractionService;

    @Value("${app.imap.initial-days-window:7}")
    private int initialDaysWindow = 7;

    /**
     * Validates IMAP credentials (email + 16-character App Password) against the mail server,
     * encrypts the credential with AES-256-GCM, and activates the email account.
     */
    @Transactional
    public CuentaCorreoDto.Response connectImapAccount(Long userId, EmailSyncDto.ConnectImapRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String sanitizedPassword = GmailImapReader.sanitizeAppPassword(request.appPassword());
        ProveedorCorreo provider = request.provider() != null ? request.provider() : ProveedorCorreo.GMAIL;

        EmailReaderProvider reader = resolveReader(provider);
        reader.validateCredentials(normalizedEmail, sanitizedPassword);

        CuentaCorreo account = cuentaCorreoRepository
                .findByUsuarioIdAndEmailCuentaIgnoreCase(userId, normalizedEmail)
                .orElse(null);

        if (account == null) {
            Usuario user = usuarioRepository.findById(userId)
                    .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

            account = CuentaCorreo.builder()
                    .usuario(user)
                    .proveedor(provider)
                    .emailCuenta(normalizedEmail)
                    .build();
        }

        account.setProveedor(provider);
        account.setEmailCuenta(normalizedEmail);
        account.setRefreshTokenCifrado(tokenCipherService.encrypt(sanitizedPassword));
        account.setEstadoConexion(EstadoConexion.ACTIVA);
        account.setFechaConexion(Instant.now());
        account.setFechaDesconexion(null);
        account.setCursorSincronizacion(null);
        account.setLastUid(null);
        account.setUidValidity(null);
        account.setLastSyncAt(null);
        account.setSyncStatus(SyncStatus.IDLE);

        return CuentaCorreoDto.Response.from(cuentaCorreoRepository.save(account));
    }

    /**
     * Synchronizes a single email account belonging to the authenticated user.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public EmailSyncDto.SyncResultDto syncAccount(Long accountId, Long userId) {
        CuentaCorreo account = cuentaCorreoRepository.findByIdAndUsuarioId(accountId, userId)
                .orElseThrow(() -> ApiException.notFound("Cuenta de correo no encontrada"));

        if (account.getEstadoConexion() != EstadoConexion.ACTIVA || account.getRefreshTokenCifrado() == null) {
            throw ApiException.badRequest(
                    "La cuenta de correo no está activa o no tiene credenciales configuradas para sincronizar");
        }

        account.setSyncStatus(SyncStatus.SYNCING);
        cuentaCorreoRepository.save(account);

        EmailReaderProvider reader = resolveReader(account.getProveedor());
        int windowDays = initialDaysWindow > 0 ? initialDaysWindow : 7;
        Instant since = account.getLastSyncAt() != null
                ? account.getLastSyncAt()
                : Instant.now().minus(windowDays, ChronoUnit.DAYS);

        try {
            EmailSyncDto.FetchBatchResult batch = reader.fetchNewEmails(account, since);
            EmailSyncDto.ExtractionSummaryDto extraction =
                    candidacyExtractionService.processEmails(userId, batch.matchedEmails());

            Instant now = Instant.now();
            account.setLastSyncAt(now);
            account.setUidValidity(batch.uidValidity());
            if (batch.highestUid() != null) {
                account.setLastUid(batch.highestUid());
                account.setCursorSincronizacion(String.valueOf(batch.highestUid()));
            }
            account.setSyncStatus(SyncStatus.SUCCESS);
            account.setEstadoConexion(EstadoConexion.ACTIVA);
            cuentaCorreoRepository.save(account);

            return new EmailSyncDto.SyncResultDto(
                    account.getId(),
                    batch.emailsScanned(),
                    extraction.candidaciesCreated(),
                    extraction.candidaciesUpdated(),
                    now,
                    SyncStatus.SUCCESS.name(),
                    extraction.detectedEmails()
            );

        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.UNAUTHORIZED) {
                account.setEstadoConexion(EstadoConexion.CON_ERROR);
            }
            account.setSyncStatus(SyncStatus.ERROR);
            cuentaCorreoRepository.save(account);
            throw e;
        } catch (Exception e) {
            account.setSyncStatus(SyncStatus.ERROR);
            cuentaCorreoRepository.save(account);
            throw ApiException.oauth("Error inesperado durante la sincronización: " + e.getMessage());
        }
    }

    /**
     * Synchronizes all active email accounts assigned to the authenticated user on demand.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public EmailSyncDto.MultiAccountSyncResultDto syncAllUserAccounts(Long userId) {
        List<CuentaCorreo> activeAccounts = cuentaCorreoRepository
                .findByUsuarioIdAndEstadoConexionIn(userId, List.of(EstadoConexion.ACTIVA));

        int totalScanned = 0;
        int totalCreated = 0;
        int totalUpdated = 0;
        List<EmailSyncDto.SyncResultDto> results = new ArrayList<>();

        for (CuentaCorreo account : activeAccounts) {
            try {
                EmailSyncDto.SyncResultDto result = syncAccount(account.getId(), userId);
                totalScanned += result.emailsScanned();
                totalCreated += result.candidaciesCreated();
                totalUpdated += result.candidaciesUpdated();
                results.add(result);
            } catch (Exception e) {
                log.warn("On-demand multi-account sync failed for account {} (user {}): {}",
                        account.getId(), userId, e.getMessage());
            }
        }

        return new EmailSyncDto.MultiAccountSyncResultDto(
                results.size(),
                totalScanned,
                totalCreated,
                totalUpdated,
                Instant.now(),
                results
        );
    }

    /**
     * Triggered asynchronously upon user login. Synchronizes each active account
     * only if it has not yet been synchronized today (UTC calendar day).
     */
    @Transactional
    public void syncOnFirstDailyLogin(Long userId) {
        List<CuentaCorreo> activeAccounts = cuentaCorreoRepository
                .findByUsuarioIdAndEstadoConexionIn(userId, List.of(EstadoConexion.ACTIVA));

        if (activeAccounts.isEmpty()) {
            return;
        }

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        for (CuentaCorreo account : activeAccounts) {
            if (isAlreadySyncedToday(account.getLastSyncAt(), todayUtc)) {
                log.debug("Skipping daily login sync for account {}: already synced today at {}",
                        account.getId(), account.getLastSyncAt());
                continue;
            }

            try {
                syncAccount(account.getId(), userId);
                log.info("Completed first daily login sync for account {} (user {})", account.getId(), userId);
            } catch (Exception e) {
                log.warn("First daily login sync failed for account {} (user {}): {}",
                        account.getId(), userId, e.getMessage());
            }
        }
    }

    private boolean isAlreadySyncedToday(Instant lastSyncAt, LocalDate todayUtc) {
        if (lastSyncAt == null) {
            return false;
        }
        LocalDate lastSyncDate = lastSyncAt.atZone(ZoneOffset.UTC).toLocalDate();
        return !lastSyncDate.isBefore(todayUtc);
    }

    private EmailReaderProvider resolveReader(ProveedorCorreo provider) {
        return emailReaderProviders.stream()
                .filter(r -> r.supports(provider))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("Proveedor de correo no soportado: " + provider));
    }
}

