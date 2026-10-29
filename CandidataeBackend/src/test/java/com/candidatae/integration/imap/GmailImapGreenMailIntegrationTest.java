package com.candidatae.integration.imap;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.EstadoConexion;
import com.candidatae.domain.ProveedorCorreo;
import com.candidatae.dto.EmailSyncDto;
import com.candidatae.security.TokenCipherService;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test suite using an embedded in-memory IMAP server (GreenMail).
 * Validates real IMAP socket connections, authentication, envelope filtering,
 * and incremental UID synchronization without requiring external internet access.
 */
@DisplayName("GmailImapReader - Embedded GreenMail IMAP Server Integration Suite")
class GmailImapGreenMailIntegrationTest {

    private static final String TEST_EMAIL = "candidato@gmail.com";
    private static final String APP_PASSWORD_RAW = "abcd efgh ijkl mnop";
    private static final String APP_PASSWORD_CLEAN = "abcdefghijklmnop";
    private static final String AES_KEY_BASE64 = "WDvLnXIqcos+Md3ZRQZxYkyMPobDDWOENqV/0A6HJGs=";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.IMAP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser(TEST_EMAIL, TEST_EMAIL, APP_PASSWORD_CLEAN));

    private TokenCipherService tokenCipherService;
    private GmailImapReader gmailImapReader;
    private GreenMailUser mailboxUser;

    @BeforeEach
    void setUp() throws FolderException {
        greenMail.purgeEmailFromAllMailboxes();
        mailboxUser = greenMail.setUser(TEST_EMAIL, TEST_EMAIL, APP_PASSWORD_CLEAN);

        tokenCipherService = new TokenCipherService(AES_KEY_BASE64);
        ImapStoreFactory storeFactory = new ImapStoreFactory();
        gmailImapReader = new GmailImapReader(tokenCipherService, storeFactory);

        ReflectionTestUtils.setField(gmailImapReader, "imapHost", "127.0.0.1");
        ReflectionTestUtils.setField(gmailImapReader, "imapPort", greenMail.getImap().getPort());
        ReflectionTestUtils.setField(gmailImapReader, "imapProtocol", "imap");
        ReflectionTestUtils.setField(gmailImapReader, "sslEnable", false);
        ReflectionTestUtils.setField(gmailImapReader, "connectionTimeoutMs", 5000);
        ReflectionTestUtils.setField(gmailImapReader, "readTimeoutMs", 5000);
        ReflectionTestUtils.setField(gmailImapReader, "maxMessagesPerSync", 50);
    }

    private void deliverEmail(String from, String subject, String body) throws Exception {
        Session session = greenMail.getImap().createSession();
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(TEST_EMAIL));
        message.setSubject(subject, "UTF-8");
        message.setSentDate(new Date());
        message.setText(body, "UTF-8");
        message.saveChanges();
        mailboxUser.deliver(message);
    }

    @Test
    @DisplayName("Test 1: Validates IMAP connection with 16-char App Password containing spaces")
    void validatesImapConnectionWithAppPassword() {
        gmailImapReader.validateCredentials(TEST_EMAIL, APP_PASSWORD_RAW);
    }

    @Test
    @DisplayName("Test 2: Initial sync fetches only job-related emails from last 7 days, and incremental sync fetches only newly arrived emails")
    void initialAndIncrementalSyncAgainstRealImapServer() throws Exception {
        // 1. Seed 3 emails into the IMAP inbox (2 job-related, 1 unrelated newsletter)
        deliverEmail(
                "jobs-noreply@linkedin.com",
                "Tu candidatura a Backend Java Developer en TechCorp",
                "Hola, tu solicitud se ha enviado correctamente a TechCorp."
        );
        deliverEmail(
                "newsletter@supermercado.es",
                "Ofertas de la semana en alimentación",
                "Descuentos del 20% en frutas y verduras."
        );
        deliverEmail(
                "notificaciones@infojobs.net",
                "Novedades en tu proceso de selección - Entrevista",
                "Queremos conocerte y agendar una entrevista técnica contigo."
        );

        CuentaCorreo account = CuentaCorreo.builder()
                .id(1L)
                .emailCuenta(TEST_EMAIL)
                .proveedor(ProveedorCorreo.GMAIL)
                .estadoConexion(EstadoConexion.ACTIVA)
                .refreshTokenCifrado(tokenCipherService.encrypt(APP_PASSWORD_RAW))
                .build();

        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        // 2. First sync (no cursor yet -> 7-day search)
        EmailSyncDto.FetchBatchResult firstSync = gmailImapReader.fetchNewEmails(account, sevenDaysAgo);

        assertEquals(3, firstSync.emailsScanned(), "Should scan all 3 messages in the 7-day window");
        assertEquals(2, firstSync.matchedEmails().size(), "Should match only the 2 job-related emails");
        assertNotNull(firstSync.highestUid());
        assertNotNull(firstSync.uidValidity());

        // Persist the cursor on the entity as EmailSyncService does
        account.setUidValidity(firstSync.uidValidity());
        account.setLastUid(firstSync.highestUid());
        account.setLastSyncAt(Instant.now());

        // 3. Second sync immediately with no new emails -> 0 scanned
        EmailSyncDto.FetchBatchResult emptyIncrementalSync = gmailImapReader.fetchNewEmails(account, account.getLastSyncAt());
        assertEquals(0, emptyIncrementalSync.emailsScanned(), "Should not re-process already seen UIDs");
        assertTrue(emptyIncrementalSync.matchedEmails().isEmpty());

        // 4. Deliver 1 new email (rejection notification) and run incremental sync
        deliverEmail(
                "no-reply@indeed.com",
                "Actualización de tu candidatura en DataSystems",
                "Lamentamos informarte que hemos decidido no continuar con tu candidatura."
        );

        EmailSyncDto.FetchBatchResult incrementalSync = gmailImapReader.fetchNewEmails(account, account.getLastSyncAt());

        assertEquals(1, incrementalSync.emailsScanned(), "Incremental sync should scan only the 1 new email");
        assertEquals(1, incrementalSync.matchedEmails().size());
        assertEquals("Actualización de tu candidatura en DataSystems", incrementalSync.matchedEmails().get(0).subject());
        assertTrue(incrementalSync.highestUid() > firstSync.highestUid());
    }
}

