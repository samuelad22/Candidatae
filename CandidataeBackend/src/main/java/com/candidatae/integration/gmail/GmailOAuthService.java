package com.candidatae.integration.gmail;

import com.candidatae.exception.ApiException;
import com.candidatae.dto.CuentaCorreoDto;
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

import java.util.Map;

/**
 * Cliente del flujo OAuth2 de Google para vincular buzones de Gmail en modo solo lectura.
 *
 * <p><strong>Por qué no usamos {@code oauth2Login()} de Spring Security:</strong> ese
 * mecanismo sirve para <em>iniciar sesión</em> en Candidatae, y la sesión de Candidatae
 * ya está resuelta con JWT propio. Si activáramos {@code oauth2Login()}, Google pasaría
 * a ser un proveedor de login y se mezclarían dos mecanismos sobre el mismo contexto de
 * seguridad. Aquí el flujo OAuth es exclusivamente para <em>leer el buzón</em>.</p>
 *
 * <p>El flujo completo es:</p>
 * <ol>
 *   <li>El frontend pide la URL de autorización → {@link #buildAuthorizationUrl(Long)}.</li>
 *   <li>El navegador va a Google y el usuario acepta los scopes de solo lectura.</li>
 *   <li>Google redirige a nuestro callback con un {@code code} → {@link #completarVinculacion}.</li>
 * </ol>
 *
 * <p>Ningún token se registra en los logs: son secretos que dan acceso al buzón.</p>
 */
@Service
@Slf4j
public class GmailOAuthService {

    private final RestClient restClient;
    private final OAuthStateStore stateStore;

    private final String clientId;
    private final String clientSecret;
    private final String authorizationUri;
    private final String tokenUri;
    private final String userInfoUri;
    private final String revokeUri;
    private final String redirectUri;
    private final String scope;

    public GmailOAuthService(RestClient.Builder restClientBuilder,
                             OAuthStateStore stateStore,
                             @Value("${app.gmail.client-id:}") String clientId,
                             @Value("${app.gmail.client-secret:}") String clientSecret,
                             @Value("${app.gmail.authorization-uri}") String authorizationUri,
                             @Value("${app.gmail.token-uri}") String tokenUri,
                             @Value("${app.gmail.user-info-uri}") String userInfoUri,
                             @Value("${app.gmail.revoke-uri}") String revokeUri,
                             @Value("${app.gmail.redirect-uri}") String redirectUri,
                             @Value("${app.gmail.scope}") String scope) {
        this.restClient = restClientBuilder.build();
        this.stateStore = stateStore;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
        this.revokeUri = revokeUri;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    /**
     * Construye la URL de Google a la que debe redirigirse el navegador.
     *
     * <p>Se añaden dos parámetros que no son opcionales para nuestro caso:</p>
     * <ul>
     *   <li>{@code access_type=offline}: pide un refresh token (acceso sin conexión).</li>
     *   <li>{@code prompt=consent}: Google solo devuelve el refresh token la primera
     *       vez que el usuario concede el acceso. Sin este parámetro, al volver a
     *       vincular una cuenta ya autorizada, la respuesta no incluye refresh token
     *       y la vinculación se quedaría sin credenciales.</li>
     * </ul>
     *
     * @param usuarioId usuario que inicia la vinculación.
     * @return la URL y el {@code state} generado.
     */
    public CuentaCorreoDto.GmailAuthorize buildAuthorizationUrl(Long usuarioId) {
        requireConfigured();

        String state = stateStore.register(usuarioId);

        String url = UriComponentsBuilder.fromUriString(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .build()
                .encode()
                .toUriString();

        return new CuentaCorreoDto.GmailAuthorize(url, state);
    }

    /**
     * Completa la vinculación: valida el {@code state}, cambia el {@code code} por tokens
     * y averigua a qué email corresponde la bandeja.
     *
     * @param code         código de autorización que devuelve Google.
     * @param state        valor de protección anti-CSRF.
     * @param cookieState  mismo valor de state, leído de la cookie del navegador.
     * @return los datos de la vinculación.
     * @throws ApiException si el state no es válido, el code falla o Google no
     *                            devuelve refresh token.
     */
    public GmailToken completarVinculacion(String code, String state, String cookieState) {
        requireConfigured();

        // 1) El state debe existir, no haber caducado y coincidir con la cookie del
        //    navegador. Esta triple comprobación evita que alguien reutilice un
        //    enlace de vinculación robado.
        if (cookieState == null || !cookieState.equals(state)) {
            log.warn("Vinculación OAuth2 de Gmail rechazada: la cookie de state no coincide");
            throw ApiException.oauth("La solicitud de vinculación no es válida. Inténtalo de nuevo.");
        }

        Long usuarioId = stateStore.consume(state)
                .orElseThrow(() -> {
                    log.warn("Vinculación OAuth2 de Gmail rechazada: state desconocido o caducado");
                    return ApiException.oauth("La solicitud de vinculación ha caducado. Inténtalo de nuevo.");
                });

        // 2) Cambiamos el código por los tokens.
        TokenResponse tokenResponse = exchangeCode(code);

        // 3) Averiguamos el email real de la cuenta autorizada (lo da Google, no el cliente).
        String email = fetchUserEmail(tokenResponse.accessToken());

        if (tokenResponse.refreshToken() == null || tokenResponse.refreshToken().isBlank()) {
            log.warn("Google no ha devuelto refresh token para el usuario {}", usuarioId);
            throw ApiException.oauth(
                    "Google no ha devuelto un token de acceso permanente. Revisa el consentimiento de la app.");
        }

        return new GmailToken(usuarioId, email, tokenResponse.refreshToken());
    }

    /**
     * Revoca un token en Google (best effort).
     *
     * <p>Se usa al desconectar una cuenta o al reemplazarla: deja de dar acceso
     * inmediatamente, sin esperar a que caduque.</p>
     */
    public void revocarToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);

        try {
            restClient.post()
                    .uri(revokeUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Token de Google revocado correctamente");
        } catch (RestClientException e) {
            // No es crítico: aunque el revoke falle, la fila se marca como desconectada
            // y el token deja de usarse desde Candidatae.
            log.warn("No se ha podido revocar el token en Google: {}", e.getMessage());
        }
    }

    /**
     * Intercambia el código de autorización por los tokens de Google.
     */
    @SuppressWarnings("unchecked")
    private TokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            Map<String, Object> body = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                throw ApiException.oauth("Google no ha devuelto ninguna respuesta al validar la vinculación.");
            }

            return new TokenResponse(
                    (String) body.get("access_token"),
                    (String) body.get("refresh_token"));

        } catch (RestClientException e) {
            log.warn("Fallo en el intercambio del código de autorización: {}", e.getMessage());
            throw ApiException.oauth("Google ha rechazado la vinculación. Vuelve a intentarlo.");
        }
    }

    /**
     * Consulta el perfil del usuario autorizado para obtener el email real de la bandeja.
     */
    @SuppressWarnings("unchecked")
    private String fetchUserEmail(String accessToken) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            if (body == null || body.get("email") == null) {
                throw ApiException.oauth("No se ha podido obtener el email de la cuenta de Gmail.");
            }

            Object emailVerified = body.get("email_verified");
            if (Boolean.FALSE.equals(emailVerified)) {
                throw ApiException.oauth("El email de la cuenta de Gmail no está verificado.");
            }

            return ((String) body.get("email")).trim().toLowerCase();

        } catch (RestClientException e) {
            log.warn("Fallo al leer el perfil de Google: {}", e.getMessage());
            throw ApiException.oauth("No se ha podido leer el perfil de la cuenta de Google.");
        }
    }

    /**
     * Comprueba que las credenciales de Google están configuradas.
     */
    private void requireConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw ApiException.oauth(
                    "La vinculación con Gmail no está configurada en el servidor "
                            + "(faltan GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET).");
        }
    }

    /**
     * Tokens devueltos por Google.
     */
    private record TokenResponse(String accessToken, String refreshToken) {
    }

    /**
     * Resultado de una vinculación completada.
     *
     * @param usuarioId    usuario que inició la vinculación.
     * @param email        email real de la bandeja de Gmail.
     * @param refreshToken token de acceso permanente (se guarda cifrado).
     */
    public record GmailToken(Long usuarioId, String email, String refreshToken) {
    }

}
