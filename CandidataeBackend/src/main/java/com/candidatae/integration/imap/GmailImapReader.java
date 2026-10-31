package com.candidatae.integration.imap;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.ProveedorCorreo;
import com.candidatae.dto.EmailSyncDto;
import com.candidatae.exception.ApiException;
import com.candidatae.security.TokenCipherService;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * IMAP implementation of {@link EmailReaderProvider} for Gmail accounts using
 * 16-character App Passwords.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GmailImapReader implements EmailReaderProvider {

    private static final String INBOX_FOLDER = "INBOX";

    private final TokenCipherService tokenCipherService;
    private final ImapStoreFactory imapStoreFactory;

    @Value("${app.imap.gmail.host:imap.gmail.com}")
    private String imapHost;

    @Value("${app.imap.gmail.port:993}")
    private int imapPort;

    @Value("${app.imap.gmail.protocol:imaps}")
    private String imapProtocol;

    @Value("${app.imap.gmail.ssl-enable:true}")
    private boolean sslEnable;

    @Value("${app.imap.connection-timeout-ms:10000}")
    private int connectionTimeoutMs;

    @Value("${app.imap.read-timeout-ms:15000}")
    private int readTimeoutMs;

    @Value("${app.imap.max-messages-per-sync:50}")
    private int maxMessagesPerSync;

    @Override
    public boolean supports(ProveedorCorreo provider) {
        return provider == ProveedorCorreo.GMAIL;
    }

    @Override
    public void validateCredentials(String email, String rawCredential) {
        String cleanPassword = sanitizeAppPassword(rawCredential);
        Store store = null;
        try {
            store = imapStoreFactory.createStore(buildImapProperties(), imapProtocol);
            store.connect(imapHost, imapPort, email, cleanPassword);
        } catch (AuthenticationFailedException e) {
            log.warn("IMAP authentication failed for account {}: {}", email, e.getMessage());
            throw ApiException.unauthorized(
                    "Credenciales IMAP inválidas. Verifica el correo y la contraseña de aplicación de 16 caracteres.");
        } catch (MessagingException e) {
            log.error("IMAP connection error while validating credentials for {}: {}", email, e.getMessage());
            throw ApiException.oauth("No se pudo conectar con el servidor IMAP: " + e.getMessage());
        } finally {
            closeQuietly(null, store);
        }
    }

    @Override
    public EmailSyncDto.FetchBatchResult fetchNewEmails(CuentaCorreo account, Instant since) {
        if (account.getRefreshTokenCifrado() == null || account.getRefreshTokenCifrado().length == 0) {
            throw ApiException.badRequest("La cuenta de correo no tiene credenciales configuradas");
        }

        String rawPassword = tokenCipherService.decrypt(account.getRefreshTokenCifrado());
        String cleanPassword = sanitizeAppPassword(rawPassword);

        Store store = null;
        Folder inbox = null;
        try {
            store = imapStoreFactory.createStore(buildImapProperties(), imapProtocol);
            store.connect(imapHost, imapPort, account.getEmailCuenta(), cleanPassword);

            inbox = store.getFolder(INBOX_FOLDER);
            inbox.open(Folder.READ_ONLY);

            UIDFolder uidFolder = (inbox instanceof UIDFolder uf) ? uf : null;
            Long currentUidValidity = uidFolder != null ? uidFolder.getUIDValidity() : null;

            boolean canUseIncrementalUid = uidFolder != null
                    && account.getLastUid() != null
                    && (account.getUidValidity() == null || account.getUidValidity().equals(currentUidValidity));

            Message[] messages;
            if (canUseIncrementalUid) {
                messages = uidFolder.getMessagesByUID(account.getLastUid() + 1, UIDFolder.LASTUID);
            } else {
                SearchTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GE, Date.from(since));
                messages = inbox.search(dateTerm);
            }

            if (messages == null || messages.length == 0) {
                return new EmailSyncDto.FetchBatchResult(
                        currentUidValidity,
                        account.getLastUid(),
                        0,
                        List.of()
                );
            }

            if (messages.length > maxMessagesPerSync) {
                messages = Arrays.copyOfRange(messages, 0, maxMessagesPerSync);
            }

            FetchProfile fetchProfile = new FetchProfile();
            fetchProfile.add(FetchProfile.Item.ENVELOPE);
            if (uidFolder != null) {
                fetchProfile.add(UIDFolder.FetchProfileItem.UID);
            }
            inbox.fetch(messages, fetchProfile);

            Long highestUid = account.getLastUid();
            int scannedCount = 0;
            List<EmailSyncDto.RawEmailDto> matchedEmails = new ArrayList<>();

            for (Message message : messages) {
                long uid = uidFolder != null ? uidFolder.getUID(message) : message.getMessageNumber();

                // RFC 3501 quirk: UID FETCH (lastUid+1):* returns lastUid when no newer messages exist
                if (canUseIncrementalUid && account.getLastUid() != null && uid <= account.getLastUid()) {
                    continue;
                }

                scannedCount++;
                if (highestUid == null || uid > highestUid) {
                    highestUid = uid;
                }

                String fromAddress = extractFromAddress(message);
                String subject = message.getSubject() != null ? message.getSubject() : "";

                if (!MimeMessageParser.isJobRelatedEmail(fromAddress, subject)) {
                    continue;
                }

                Date emailDate = message.getSentDate() != null
                        ? message.getSentDate()
                        : message.getReceivedDate();
                Instant receivedAt = emailDate != null ? emailDate.toInstant() : Instant.now();

                String[] messageIdHeader = message.getHeader("Message-ID");
                String messageId = (messageIdHeader != null && messageIdHeader.length > 0)
                        ? messageIdHeader[0]
                        : "uid-" + uid;

                MimeMessageParser.ParsedContent body = MimeMessageParser.extractContent(message);

                matchedEmails.add(new EmailSyncDto.RawEmailDto(
                        uid,
                        messageId,
                        fromAddress,
                        subject,
                        receivedAt,
                        body.plainText(),
                        body.htmlText()
                ));
            }

            matchedEmails.sort(Comparator.comparing(
                    EmailSyncDto.RawEmailDto::receivedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));

            return new EmailSyncDto.FetchBatchResult(
                    currentUidValidity,
                    highestUid,
                    scannedCount,
                    matchedEmails
            );

        } catch (AuthenticationFailedException e) {
            log.warn("IMAP authentication failed during sync for account {}: {}", account.getEmailCuenta(), e.getMessage());
            throw ApiException.unauthorized(
                    "Fallo de autenticación IMAP: la contraseña de aplicación es inválida o fue revocada");
        } catch (Exception e) {
            log.error("Error reading emails via IMAP for account {}: {}", account.getEmailCuenta(), e.getMessage());
            if (e instanceof ApiException apiEx) {
                throw apiEx;
            }
            throw ApiException.oauth("Error durante la sincronización IMAP: " + e.getMessage());
        } finally {
            closeQuietly(inbox, store);
        }
    }

    /**
     * Removes spaces from Google's 16-character App Passwords (often copied as "xxxx yyyy zzzz wwww").
     */
    public static String sanitizeAppPassword(String rawPassword) {
        if (rawPassword == null) {
            return "";
        }
        return rawPassword.replaceAll("\\s+", "");
    }

    private Properties buildImapProperties() {
        Properties props = new Properties();
        props.put("mail.store.protocol", imapProtocol);
        props.put("mail." + imapProtocol + ".host", imapHost);
        props.put("mail." + imapProtocol + ".port", String.valueOf(imapPort));
        props.put("mail." + imapProtocol + ".ssl.enable", String.valueOf(sslEnable));
        props.put("mail." + imapProtocol + ".connectiontimeout", String.valueOf(connectionTimeoutMs));
        props.put("mail." + imapProtocol + ".timeout", String.valueOf(readTimeoutMs));
        return props;
    }

    private String extractFromAddress(Message message) throws MessagingException {
        Address[] froms = message.getFrom();
        if (froms == null || froms.length == 0) {
            return "";
        }
        if (froms[0] instanceof InternetAddress internetAddress) {
            return internetAddress.getAddress() != null
                    ? internetAddress.getAddress()
                    : internetAddress.toString();
        }
        return froms[0].toString();
    }

    private void closeQuietly(Folder folder, Store store) {
        if (folder != null) {
            try {
                if (folder.isOpen()) {
                    folder.close(false);
                }
            } catch (Exception e) {
                log.debug("Ignored error closing IMAP folder: {}", e.getMessage());
            }
        }
        if (store != null) {
            try {
                if (store.isConnected()) {
                    store.close();
                }
            } catch (Exception e) {
                log.debug("Ignored error closing IMAP store: {}", e.getMessage());
            }
        }
    }
}

