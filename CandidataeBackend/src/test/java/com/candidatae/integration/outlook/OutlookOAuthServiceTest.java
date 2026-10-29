package com.candidatae.integration.outlook;

import com.candidatae.dto.CuentaCorreoDto;
import com.candidatae.exception.ApiException;
import com.candidatae.integration.gmail.OAuthStateStore;
import com.candidatae.integration.imap.OutlookImapReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OutlookOAuthService - tests del flujo OAuth2 con Microsoft")
class OutlookOAuthServiceTest {

    private OAuthStateStore stateStore;
    private OutlookOAuthService service;

    @BeforeEach
    void setUp() {
        stateStore = new OAuthStateStore(600);
        service = new OutlookOAuthService(
                RestClient.builder(),
                stateStore,
                "test-client-id",
                "test-client-secret",
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                "https://graph.microsoft.com/v1.0/me",
                "https://graph.microsoft.com/v1.0/me/mailFolders/Inbox/messages",
                "http://localhost:8080/api/email-accounts/outlook/callback",
                "offline_access User.Read Mail.Read"
        );
    }

    @Test
    @DisplayName("buildAuthorizationUrl construye la URL correcta de Microsoft con client_id, state y scopes")
    void buildAuthorizationUrlGeneratesValidUrl() {
        Long usuarioId = 42L;
        CuentaCorreoDto.OutlookAuthorize auth = service.buildAuthorizationUrl(usuarioId);

        assertNotNull(auth);
        assertNotNull(auth.authorizationUrl());
        assertNotNull(auth.state());
        assertTrue(auth.authorizationUrl().startsWith("https://login.microsoftonline.com/common/oauth2/v2.0/authorize"));
        assertTrue(auth.authorizationUrl().contains("client_id=test-client-id"));
        assertTrue(auth.authorizationUrl().contains("state=" + auth.state()));
        assertTrue(auth.authorizationUrl().contains("scope=offline_access") && auth.authorizationUrl().contains("Mail.Read"));
        assertTrue(auth.authorizationUrl().contains("prompt=select_account"));
    }

    @Test
    @DisplayName("completarVinculacion falla si la cookie no coincide con el state")
    void completarVinculacionRechazaCookieInvalida() {
        ApiException ex = assertThrows(ApiException.class, () ->
                service.completarVinculacion("code123", "state-abc", "state-mismatch"));

        assertTrue(ex.getMessage().contains("no es válida"));
    }

    @Test
    @DisplayName("completarVinculacion falla si el state no existe o ha caducado")
    void completarVinculacionRechazaStateCaducado() {
        ApiException ex = assertThrows(ApiException.class, () ->
                service.completarVinculacion("code123", "unknown-state", "unknown-state"));

        assertTrue(ex.getMessage().contains("ha caducado"));
    }

    @Test
    @DisplayName("requireConfigured lanza excepción si faltan credenciales de Microsoft")
    void requireConfiguredValidaCredenciales() {
        OutlookOAuthService unconfigured = new OutlookOAuthService(
                RestClient.builder(),
                stateStore,
                "",
                "",
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                "https://graph.microsoft.com/v1.0/me",
                "https://graph.microsoft.com/v1.0/me/mailFolders/Inbox/messages",
                "http://localhost:8080/api/email-accounts/outlook/callback",
                "offline_access User.Read Mail.Read"
        );

        assertThrows(ApiException.class, unconfigured::requireConfigured);
    }

    @Test
    @DisplayName("OutlookImapReader detecta correctamente si una credencial es OAuth o Contraseña de Aplicación IMAP")
    void outlookImapReaderDistingueOAuthVsAppPassword() {
        // Contraseñas de aplicación IMAP (16 caracteres, cortas)
        assertFalse(OutlookImapReader.looksLikeOAuthToken("abcd efgh ijkl mnop"));
        assertFalse(OutlookImapReader.looksLikeOAuthToken("abcdefghijklmnop"));
        assertFalse(OutlookImapReader.looksLikeOAuthToken("MyPassword123"));

        // Tokens OAuth2 (largos, con puntos o prefijo M.)
        assertTrue(OutlookImapReader.looksLikeOAuthToken("M.C544_BAY.0.U.-Cpv394u..."));
        assertTrue(OutlookImapReader.looksLikeOAuthToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.longpayloadcontent.signaturehere"));
        assertTrue(OutlookImapReader.looksLikeOAuthToken("a".repeat(60)));
    }
}

