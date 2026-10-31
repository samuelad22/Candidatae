package com.candidatae.integration.imap;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.EstadoConexion;
import com.candidatae.domain.ProveedorCorreo;
import com.candidatae.dto.EmailSyncDto;
import com.candidatae.exception.ApiException;
import com.candidatae.security.TokenCipherService;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.SearchTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailImapReader - unit tests for IMAP email retrieval and UID cursors")
class GmailImapReaderTest {

    @Mock
    private TokenCipherService tokenCipherService;

    @Mock
    private ImapStoreFactory imapStoreFactory;

    @Mock
    private Store store;

    private Folder uidInbox;

    private GmailImapReader reader;

    @BeforeEach
    void setUp() {
        reader = new GmailImapReader(tokenCipherService, imapStoreFactory);
        ReflectionTestUtils.setField(reader, "imapHost", "imap.gmail.com");
        ReflectionTestUtils.setField(reader, "imapPort", 993);
        ReflectionTestUtils.setField(reader, "imapProtocol", "imaps");
        ReflectionTestUtils.setField(reader, "sslEnable", true);
        ReflectionTestUtils.setField(reader, "connectionTimeoutMs", 10000);
        ReflectionTestUtils.setField(reader, "readTimeoutMs", 15000);
        ReflectionTestUtils.setField(reader, "maxMessagesPerSync", 50);

        uidInbox = mock(Folder.class, withSettings().extraInterfaces(UIDFolder.class));
    }

    @Test
    @DisplayName("supports() returns true only for GMAIL")
    void supportsReturnsTrueForGmailOnly() {
        assertTrue(reader.supports(ProveedorCorreo.GMAIL));
        assertFalse(reader.supports(ProveedorCorreo.OUTLOOK));
    }

    @Test
    @DisplayName("sanitizeAppPassword() strips spaces from 16-character Google App Passwords")
    void sanitizeAppPasswordRemovesWhitespace() {
        assertEquals("abcdefghijklmnop", GmailImapReader.sanitizeAppPassword("abcd efgh ijkl mnop"));
        assertEquals("abcdefghijklmnop", GmailImapReader.sanitizeAppPassword("  abcd   efgh\tijkl\nmnop "));
    }

    @Test
    @DisplayName("validateCredentials() connects with sanitized password and closes store")
    void validateCredentialsSuccess() throws Exception {
        when(imapStoreFactory.createStore(any(Properties.class), eq("imaps"))).thenReturn(store);
        when(store.isConnected()).thenReturn(true);

        reader.validateCredentials("user@gmail.com", "abcd efgh ijkl mnop");

        verify(store).connect("imap.gmail.com", 993, "user@gmail.com", "abcdefghijklmnop");
        verify(store).close();
    }

    @Test
    @DisplayName("validateCredentials() throws 401 ApiException when AuthenticationFailedException occurs")
    void validateCredentialsThrows401OnAuthFailure() throws Exception {
        when(imapStoreFactory.createStore(any(Properties.class), eq("imaps"))).thenReturn(store);
        doThrow(new AuthenticationFailedException("Invalid credentials"))
                .when(store).connect(anyString(), anyInt(), anyString(), anyString());

        ApiException ex = assertThrows(ApiException.class,
                () -> reader.validateCredentials("user@gmail.com", "bad-pass"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("fetchNewEmails() uses 7-day date search when lastUid is null and filters non-job emails")
    void fetchNewEmailsInitialDateSearch() throws Exception {
        CuentaCorreo account = CuentaCorreo.builder()
                .id(1L)
                .emailCuenta("user@gmail.com")
                .refreshTokenCifrado("encrypted".getBytes())
                .estadoConexion(EstadoConexion.ACTIVA)
                .lastUid(null)
                .build();

        when(tokenCipherService.decrypt(any())).thenReturn("abcd efgh ijkl mnop");
        when(imapStoreFactory.createStore(any(Properties.class), eq("imaps"))).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(uidInbox);

        UIDFolder uf = (UIDFolder) uidInbox;
        when(uf.getUIDValidity()).thenReturn(999L);

        Message jobMessage = mock(Message.class);
        when(uf.getUID(jobMessage)).thenReturn(10L);
        when(jobMessage.getFrom()).thenReturn(new Address[]{new InternetAddress("jobs-listings@linkedin.com")});
        when(jobMessage.getSubject()).thenReturn("Tu candidatura a Java Backend Developer");
        when(jobMessage.getReceivedDate()).thenReturn(new Date());
        when(jobMessage.isMimeType("text/plain")).thenReturn(true);
        when(jobMessage.getContent()).thenReturn("Gracias por inscribirte en la oferta.");

        Message spamMessage = mock(Message.class);
        when(uf.getUID(spamMessage)).thenReturn(11L);
        when(spamMessage.getFrom()).thenReturn(new Address[]{new InternetAddress("promo@tienda.com")});
        when(spamMessage.getSubject()).thenReturn("Descuentos de temporada");

        when(uidInbox.search(any(SearchTerm.class))).thenReturn(new Message[]{jobMessage, spamMessage});

        EmailSyncDto.FetchBatchResult result = reader.fetchNewEmails(account, Instant.now().minusSeconds(604800));

        assertEquals(999L, result.uidValidity());
        assertEquals(11L, result.highestUid());
        assertEquals(2, result.emailsScanned());
        assertEquals(1, result.matchedEmails().size());
        assertEquals("Tu candidatura a Java Backend Developer", result.matchedEmails().get(0).subject());
        verify(spamMessage, never()).getContent();
    }

    @Test
    @DisplayName("fetchNewEmails() uses incremental UID range and ignores messages <= lastUid (RFC 3501 quirk)")
    void fetchNewEmailsIncrementalUidRange() throws Exception {
        CuentaCorreo account = CuentaCorreo.builder()
                .id(1L)
                .emailCuenta("user@gmail.com")
                .refreshTokenCifrado("encrypted".getBytes())
                .estadoConexion(EstadoConexion.ACTIVA)
                .uidValidity(999L)
                .lastUid(100L)
                .build();

        when(tokenCipherService.decrypt(any())).thenReturn("abcdefghijklmnop");
        when(imapStoreFactory.createStore(any(Properties.class), eq("imaps"))).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(uidInbox);

        UIDFolder uf = (UIDFolder) uidInbox;
        when(uf.getUIDValidity()).thenReturn(999L);

        Message alreadyProcessedMsg = mock(Message.class);
        when(uf.getUID(alreadyProcessedMsg)).thenReturn(100L);

        Message newMsg = mock(Message.class);
        when(uf.getUID(newMsg)).thenReturn(105L);
        when(newMsg.getFrom()).thenReturn(new Address[]{new InternetAddress("no-reply@infojobs.net")});
        when(newMsg.getSubject()).thenReturn("Estado de tu inscripción: entrevista programada");
        when(newMsg.getReceivedDate()).thenReturn(new Date());
        when(newMsg.isMimeType("text/plain")).thenReturn(true);
        when(newMsg.getContent()).thenReturn("Te citamos a entrevista técnica.");

        when(uf.getMessagesByUID(101L, UIDFolder.LASTUID))
                .thenReturn(new Message[]{alreadyProcessedMsg, newMsg});

        EmailSyncDto.FetchBatchResult result = reader.fetchNewEmails(account, Instant.now());

        verify(uf).getMessagesByUID(101L, UIDFolder.LASTUID);
        verify(uidInbox, never()).search(any());
        assertEquals(1, result.emailsScanned());
        assertEquals(105L, result.highestUid());
        assertEquals(1, result.matchedEmails().size());
    }

    @Test
    @DisplayName("fetchNewEmails() falls back to 7-day date search when UIDVALIDITY changes on server")
    void fetchNewEmailsFallbackWhenUidValidityChanges() throws Exception {
        CuentaCorreo account = CuentaCorreo.builder()
                .id(1L)
                .emailCuenta("user@gmail.com")
                .refreshTokenCifrado("encrypted".getBytes())
                .estadoConexion(EstadoConexion.ACTIVA)
                .uidValidity(100L)
                .lastUid(50L)
                .build();

        when(tokenCipherService.decrypt(any())).thenReturn("abcdefghijklmnop");
        when(imapStoreFactory.createStore(any(Properties.class), eq("imaps"))).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(uidInbox);

        UIDFolder uf = (UIDFolder) uidInbox;
        when(uf.getUIDValidity()).thenReturn(200L); // Changed on server!
        when(uidInbox.search(any(SearchTerm.class))).thenReturn(new Message[0]);

        EmailSyncDto.FetchBatchResult result = reader.fetchNewEmails(account, Instant.now());

        verify(uidInbox).search(any(SearchTerm.class));
        verify(uf, never()).getMessagesByUID(anyLong(), anyLong());
        assertEquals(200L, result.uidValidity());
    }

    @Test
    @DisplayName("fetchNewEmails() processes batch from index 0 to avoid permanent email skipping")
    void fetchNewEmailsTruncatesFromStart() throws Exception {
        CuentaCorreo account = CuentaCorreo.builder()
                .id(1L)
                .emailCuenta("user@gmail.com")
                .refreshTokenCifrado("encrypted".getBytes())
                .estadoConexion(EstadoConexion.ACTIVA)
                .build();

        when(tokenCipherService.decrypt(any())).thenReturn("abcdefghijklmnop");
        when(imapStoreFactory.createStore(any(Properties.class), eq("imaps"))).thenReturn(store);
        when(store.getFolder("INBOX")).thenReturn(uidInbox);

        UIDFolder uf = (UIDFolder) uidInbox;
        when(uf.getUIDValidity()).thenReturn(999L);

        ReflectionTestUtils.setField(reader, "maxMessagesPerSync", 2);

        Message msg1 = mock(Message.class);
        when(uf.getUID(msg1)).thenReturn(1L);
        when(msg1.getFrom()).thenReturn(new Address[]{new InternetAddress("alert@indeed.com")});
        when(msg1.getSubject()).thenReturn("Confirmación de tu postulación");
        when(msg1.getSentDate()).thenReturn(new Date());
        when(msg1.isMimeType("text/plain")).thenReturn(true);
        when(msg1.getContent()).thenReturn("Postulación completada");

        Message msg2 = mock(Message.class);
        when(uf.getUID(msg2)).thenReturn(2L);
        when(msg2.getFrom()).thenReturn(new Address[]{new InternetAddress("alert@indeed.com")});
        when(msg2.getSubject()).thenReturn("Confirmación de tu candidatura");
        when(msg2.getSentDate()).thenReturn(new Date());
        when(msg2.isMimeType("text/plain")).thenReturn(true);
        when(msg2.getContent()).thenReturn("Candidatura recibida");

        Message msg3 = mock(Message.class);

        when(uidInbox.search(any(SearchTerm.class))).thenReturn(new Message[]{msg1, msg2, msg3});

        EmailSyncDto.FetchBatchResult result = reader.fetchNewEmails(account, Instant.now());

        assertEquals(2, result.emailsScanned(), "Should scan only maxMessagesPerSync (2)");
        assertEquals(2L, result.highestUid(), "Highest UID should be message 2, leaving message 3 for the next incremental sync");
        verify(msg3, never()).getSubject();
    }
}

