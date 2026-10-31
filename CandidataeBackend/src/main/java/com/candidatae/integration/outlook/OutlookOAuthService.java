package com.candidatae.integration.outlook;

import com.candidatae.dto.CuentaCorreoDto;
import com.candidatae.dto.EmailSyncDto;
import com.candidatae.exception.ApiException;
import com.candidatae.integration.gmail.OAuthStateStore;
import com.candidatae.integration.imap.MimeMessageParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cliente del flujo OAuth2 de Microsoft / Entra ID para vincular y sincronizar buzones de Outlook
 * y Office 365 con permisos de lectura (offline_access, User.Read, Mail.Read).
 */
@Service
@Slf4j
public class OutlookOAuthService {

    private final RestClient restClient;
    private final OAuthStateStore stateStore;

    private final String clientId;
    private final String clientSecret;
    private final String authorizationUri;
    private final String tokenUri;
    private final String userInfoUri;
    private final String messagesUri;
    private final String redirectUri;
    private final String scope;

    public OutlookOAuthService(RestClient.Builder restClientBuilder,
                               OAuthStateStore stateStore,
                               @Value("${app.outlook.client-id:}") String clientId,
                               @Value("${app.outlook.client-secret:}") String clientSecret,
                               @Value("${app.outlook.authorization-uri:https://login.microsoftonline.com/common/oauth2/v2.0/authorize}") String authorizationUri,
                               @Value("${app.outlook.token-uri:https://login.microsoftonline.com/common/oauth2/v2.0/token}") String tokenUri,
                               @Value("${app.outlook.user-info-uri:https://graph.microsoft.com/v1.0/me}") String userInfoUri,
                               @Value("${app.outlook.messages-uri:https://graph.microsoft.com/v1.0/me/mailFolders/Inbox/messages}") String messagesUri,
                               @Value("${app.outlook.redirect-uri:http://localhost:8080/api/email-accounts/outlook/callback}") String redirectUri,
                               @Value("${app.outlook.scope:offline_access User.Read Mail.Read}") String scope) {
        this.restClient = restClientBuilder.build();
        this.stateStore = stateStore;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
        this.messagesUri = messagesUri;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    /**
     * Construye la URL de Microsoft a la que debe redirigirse el usuario.
     */
    public CuentaCorreoDto.OutlookAuthorize buildAuthorizationUrl(Long usuarioId) {
        requireConfigured();

        String state = stateStore.register(usuarioId);

        String url = UriComponentsBuilder.fromUriString(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_mode", "query")
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("prompt", "select_account")
                .build()
                .encode()
                .toUriString();

        return new CuentaCorreoDto.OutlookAuthorize(url, state);
    }

    /**
     * Completa la vinculación OAuth2 de Outlook: valida el state y la cookie,
     * intercambia el code por tokens e invoca Microsoft Graph para obtener el email.
     */
    public OutlookToken completarVinculacion(String code, String state, String cookieState) {
        requireConfigured();

        if (cookieState == null || !cookieState.equals(state)) {
            log.warn("Vinculación OAuth2 de Outlook rechazada: la cookie de state no coincide");
            throw ApiException.oauth("La solicitud de vinculación no es válida. Inténtalo de nuevo.");
        }

        Long usuarioId = stateStore.consume(state)
                .orElseThrow(() -> {
                    log.warn("Vinculación OAuth2 de Outlook rechazada: state desconocido o caducado");
                    return ApiException.oauth("La solicitud de vinculación ha caducado. Inténtalo de nuevo.");
                });

        TokenResponse tokenResponse = exchangeCode(code);
        String email = fetchUserEmail(tokenResponse.accessToken());

        if (tokenResponse.refreshToken() == null || tokenResponse.refreshToken().isBlank()) {
            log.warn("Microsoft no ha devuelto refresh token para el usuario {}", usuarioId);
            throw ApiException.oauth(
                    "Microsoft no ha devuelto un token de acceso permanente. Revisa los permisos concedidos.");
        }

        return new OutlookToken(usuarioId, email, tokenResponse.refreshToken());
    }

    /**
     * Obtiene un access_token fresco a partir de un refresh_token de Microsoft.
     */
    @SuppressWarnings("unchecked")
    public String refreshAccessToken(String refreshToken) {
        requireConfigured();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        form.add("scope", scope);

        try {
            Map<String, Object> body = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (body == null || body.get("access_token") == null) {
                throw ApiException.oauth("No se pudo renovar el token de acceso de Microsoft.");
            }
            return (String) body.get("access_token");
        } catch (RestClientException e) {
            log.warn("Error renovando token de acceso de Microsoft: {}", e.getMessage());
            throw ApiException.unauthorized("El token de acceso de Microsoft ha expirado o fue revocado.");
        }
    }

    /**
     * Sincroniza correos electrónicos de la bandeja de entrada mediante Microsoft Graph API.
     */
    @SuppressWarnings("unchecked")
    public EmailSyncDto.FetchBatchResult fetchEmailsViaGraph(String refreshToken, Instant since, int maxMessages) {
        String accessToken = refreshAccessToken(refreshToken);
        int top = (maxMessages > 0 && maxMessages <= 50) ? maxMessages : 50;

        String url = UriComponentsBuilder.fromUriString(messagesUri)
                .queryParam("$top", top)
                .queryParam("$select", "id,subject,from,receivedDateTime,body,bodyPreview")
                .queryParam("$orderby", "receivedDateTime desc")
                .build()
                .toUriString();

        try {
            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("value")) {
                return new EmailSyncDto.FetchBatchResult(null, null, 0, List.of());
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
            if (items == null || items.isEmpty()) {
                return new EmailSyncDto.FetchBatchResult(null, null, 0, List.of());
            }

            List<EmailSyncDto.RawEmailDto> matchedEmails = new ArrayList<>();
            int scannedCount = 0;
            long uidCounter = 1L;

            for (Map<String, Object> item : items) {
                scannedCount++;
                String id = (String) item.get("id");
                String subject = (String) item.get("subject");
                if (subject == null) subject = "";

                String fromAddress = "";
                Map<String, Object> fromMap = (Map<String, Object>) item.get("from");
                if (fromMap != null) {
                    Map<String, Object> emailAddress = (Map<String, Object>) fromMap.get("emailAddress");
                    if (emailAddress != null) {
                        fromAddress = (String) emailAddress.get("address");
                    }
                }

                if (!MimeMessageParser.isJobRelatedEmail(fromAddress, subject)) {
                    continue;
                }

                String receivedDateTimeStr = (String) item.get("receivedDateTime");
                Instant receivedAt = Instant.now();
                if (receivedDateTimeStr != null) {
                    try {
                        receivedAt = Instant.parse(receivedDateTimeStr);
                    } catch (Exception ignored) {
                    }
                }

                String bodyPreview = (String) item.get("bodyPreview");
                String bodyHtml = "";
                Map<String, Object> bodyMap = (Map<String, Object>) item.get("body");
                if (bodyMap != null) {
                    bodyHtml = (String) bodyMap.get("content");
                }

                matchedEmails.add(new EmailSyncDto.RawEmailDto(
                        uidCounter++,
                        id != null ? id : "msg-" + uidCounter,
                        fromAddress != null ? fromAddress : "",
                        subject,
                        receivedAt,
                        bodyPreview != null ? bodyPreview : "",
                        bodyHtml != null ? bodyHtml : ""
                ));
            }

            return new EmailSyncDto.FetchBatchResult(
                    null,
                    (long) scannedCount,
                    scannedCount,
                    matchedEmails
            );

        } catch (RestClientException e) {
            log.error("Error al consultar mensajes de Microsoft Graph API: {}", e.getMessage());
            throw ApiException.oauth("Error al sincronizar correos con Microsoft Graph: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private TokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");
        form.add("scope", scope);

        try {
            Map<String, Object> body = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                throw ApiException.oauth("Microsoft no devolvió ninguna respuesta al validar la vinculación.");
            }

            return new TokenResponse(
                    (String) body.get("access_token"),
                    (String) body.get("refresh_token")
            );
        } catch (RestClientException e) {
            log.warn("Fallo en el intercambio del código de autorización de Outlook: {}", e.getMessage());
            throw ApiException.oauth("Microsoft ha rechazado la vinculación. Vuelve a intentarlo.");
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchUserEmail(String accessToken) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                throw ApiException.oauth("No se pudo leer el perfil de usuario de Microsoft.");
            }

            String mail = (String) body.get("mail");
            if (mail == null || mail.isBlank()) {
                mail = (String) body.get("userPrincipalName");
            }

            if (mail == null || mail.isBlank()) {
                throw ApiException.oauth("No se pudo obtener el email de la cuenta de Outlook.");
            }

            return mail.trim().toLowerCase();
        } catch (RestClientException e) {
            log.warn("Fallo al leer perfil de Microsoft Graph: {}", e.getMessage());
            throw ApiException.oauth("No se pudo leer la cuenta de Microsoft.");
        }
    }

    public void requireConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw ApiException.oauth(
                    "La vinculación con Outlook no está configurada en el servidor (faltan OUTLOOK_CLIENT_ID y OUTLOOK_CLIENT_SECRET).");
        }
    }

    private record TokenResponse(String accessToken, String refreshToken) {
    }

    public record OutlookToken(Long usuarioId, String email, String refreshToken) {
    }
}

