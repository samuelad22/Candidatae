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
import com.candidatae.repository.CuentaCorreoRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.TokenCipherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailSyncService - unit tests for IMAP connection, manual sync, and first daily login")
class EmailSyncServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    @Mock
    private CuentaCorreoRepository cuentaCorreoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenCipherService tokenCipherService;

    @Mock
    private EmailReaderProvider gmailReader;

    private CandidacyExtractionService candidacyExtractionService;

    private EmailSyncService emailSyncService;

    @BeforeEach
    void setUp() {
        candidacyExtractionService = new CandidacyExtractionService();
        emailSyncService = new EmailSyncService(
                cuentaCorreoRepository,
                usuarioRepository,
                tokenCipherService,
                List.of(gmailReader),
                candidacyExtractionService
        );
    }

    private CuentaCorreo activeAccount() {
        Usuario user = Usuario.builder().id(USER_ID).email("ana@correo.com").name("Ana").active(true).build();
        return CuentaCorreo.builder()
                .id(ACCOUNT_ID)
                .usuario(user)
                .proveedor(ProveedorCorreo.GMAIL)
                .emailCuenta("ana@gmail.com")
                .refreshTokenCifrado("encrypted-pass".getBytes())
                .estadoConexion(EstadoConexion.ACTIVA)
                .syncStatus(SyncStatus.IDLE)
                .build();
    }

    @Test
    @DisplayName("connectImapAccount() validates credentials, strips spaces, encrypts password, and activates account")
    void connectImapAccountSuccess() {
        when(gmailReader.supports(ProveedorCorreo.GMAIL)).thenReturn(true);
        when(cuentaCorreoRepository.findByUsuarioIdAndEmailCuentaIgnoreCase(USER_ID, "ana@gmail.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findById(USER_ID))
                .thenReturn(Optional.of(Usuario.builder().id(USER_ID).email("ana@correo.com").build()));
        when(tokenCipherService.encrypt("abcdefghijklmnop")).thenReturn("cipher".getBytes());
        when(cuentaCorreoRepository.save(any(CuentaCorreo.class))).thenAnswer(inv -> {
            CuentaCorreo c = inv.getArgument(0);
            c.setId(ACCOUNT_ID);
            return c;
        });

        EmailSyncDto.ConnectImapRequest req = new EmailSyncDto.ConnectImapRequest(
                "  Ana@Gmail.com ", "abcd efgh ijkl mnop", ProveedorCorreo.GMAIL
        );

        CuentaCorreoDto.Response response = emailSyncService.connectImapAccount(USER_ID, req);

        verify(gmailReader).validateCredentials("ana@gmail.com", "abcdefghijklmnop");
        verify(tokenCipherService).encrypt("abcdefghijklmnop");
        assertEquals("ACTIVA", response.estadoConexion());
        assertTrue(response.activa());
    }

    @Test
    @DisplayName("syncAccount() fetches new emails, classifies candidacies, and updates UID cursor")
    void syncAccountSuccess() {
        CuentaCorreo account = activeAccount();
        when(cuentaCorreoRepository.findByIdAndUsuarioId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(gmailReader.supports(ProveedorCorreo.GMAIL)).thenReturn(true);

        List<EmailSyncDto.RawEmailDto> rawEmails = List.of(
                new EmailSyncDto.RawEmailDto(
                        42L, "<msg1>", "jobs@linkedin.com",
                        "Tu candidatura a Backend Developer", Instant.now(),
                        "Gracias por inscribirte en Acme.", null
                ),
                new EmailSyncDto.RawEmailDto(
                        45L, "<msg2>", "seleccion@infojobs.net",
                        "Actualización de tu candidatura", Instant.now(),
                        "Lamentamos informarte que hemos decidido no continuar con tu candidatura.", null
                )
        );

        when(gmailReader.fetchNewEmails(eq(account), any(Instant.class)))
                .thenReturn(new EmailSyncDto.FetchBatchResult(777L, 45L, 8, rawEmails));

        EmailSyncDto.SyncResultDto result = emailSyncService.syncAccount(ACCOUNT_ID, USER_ID);

        assertEquals(8, result.emailsScanned());
        assertEquals(1, result.candidaciesCreated());
        assertEquals(1, result.candidaciesUpdated());
        assertEquals("SUCCESS", result.syncStatus());
        assertEquals(45L, account.getLastUid());
        assertEquals(777L, account.getUidValidity());
        assertEquals("45", account.getCursorSincronizacion());
        assertNotNull(account.getLastSyncAt());
        assertEquals(SyncStatus.SUCCESS, account.getSyncStatus());
    }

    @Test
    @DisplayName("syncAccount() marks account CON_ERROR and ERROR when IMAP authentication fails")
    void syncAccountMarksErrorOnAuthFailure() {
        CuentaCorreo account = activeAccount();
        when(cuentaCorreoRepository.findByIdAndUsuarioId(ACCOUNT_ID, USER_ID)).thenReturn(Optional.of(account));
        when(gmailReader.supports(ProveedorCorreo.GMAIL)).thenReturn(true);
        doThrow(ApiException.unauthorized("Invalid App Password"))
                .when(gmailReader).fetchNewEmails(eq(account), any(Instant.class));

        ApiException ex = assertThrows(ApiException.class,
                () -> emailSyncService.syncAccount(ACCOUNT_ID, USER_ID));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(EstadoConexion.CON_ERROR, account.getEstadoConexion());
        assertEquals(SyncStatus.ERROR, account.getSyncStatus());
    }

    @Test
    @DisplayName("syncOnFirstDailyLogin() skips account if already synchronized today")
    void syncOnFirstDailyLoginSkipsIfAlreadySyncedToday() {
        CuentaCorreo syncedToday = activeAccount();
        syncedToday.setLastSyncAt(Instant.now());

        when(cuentaCorreoRepository.findByUsuarioIdAndEstadoConexionIn(USER_ID, List.of(EstadoConexion.ACTIVA)))
                .thenReturn(List.of(syncedToday));

        emailSyncService.syncOnFirstDailyLogin(USER_ID);

        verify(gmailReader, never()).fetchNewEmails(any(), any());
    }

    @Test
    @DisplayName("syncOnFirstDailyLogin() synchronizes account if lastSyncAt was yesterday")
    void syncOnFirstDailyLoginExecutesWhenLastSyncWasYesterday() {
        CuentaCorreo syncedYesterday = activeAccount();
        syncedYesterday.setLastSyncAt(Instant.now().minus(2, ChronoUnit.DAYS));

        when(cuentaCorreoRepository.findByUsuarioIdAndEstadoConexionIn(USER_ID, List.of(EstadoConexion.ACTIVA)))
                .thenReturn(List.of(syncedYesterday));
        when(cuentaCorreoRepository.findByIdAndUsuarioId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(syncedYesterday));
        when(gmailReader.supports(ProveedorCorreo.GMAIL)).thenReturn(true);
        when(gmailReader.fetchNewEmails(eq(syncedYesterday), any(Instant.class)))
                .thenReturn(new EmailSyncDto.FetchBatchResult(777L, 50L, 1, List.of()));

        emailSyncService.syncOnFirstDailyLogin(USER_ID);

        verify(gmailReader).fetchNewEmails(eq(syncedYesterday), any(Instant.class));
        assertEquals(SyncStatus.SUCCESS, syncedYesterday.getSyncStatus());
    }
}

