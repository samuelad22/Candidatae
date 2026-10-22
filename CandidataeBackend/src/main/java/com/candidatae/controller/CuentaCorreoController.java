package com.candidatae.controller;

import com.candidatae.dto.CuentaCorreoDto;
import com.candidatae.exception.ApiException;
import com.candidatae.integration.gmail.GmailOAuthService;
import com.candidatae.security.CurrentUserProvider;
import com.candidatae.service.CuentaCorreoService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * API REST de las cuentas de correo del usuario ({@code /api/email-accounts}).
 *
 * <p>Todos los endpoints (salvo el callback de Google) exigen el JWT de la sesión y
 * operan <strong>siempre</strong> sobre las cuentas del usuario autenticado.</p>
 */
@RestController
@RequestMapping("/api/email-accounts")
@RequiredArgsConstructor
@Slf4j
public class CuentaCorreoController {

    /** Nombre de la cookie que ata el navegador a la solicitud de vinculación. */
    private static final String STATE_COOKIE = "gmail_link_state";

    /** Ruta del frontend donde se muestra la lista de cuentas de correo. */
    private static final String FRONTEND_CUENTAS_PATH = "/cuentas-correo";

    private final CuentaCorreoService cuentaCorreoService;
    private final GmailOAuthService gmailOAuthService;
    private final CurrentUserProvider currentUserProvider;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.gmail.state-ttl-seconds:600}")
    private long stateTtlSeconds;

    @Value("${app.gmail.secure-cookie:false}")
    private boolean secureCookie;

    /**
     * Lista las cuentas de correo del usuario autenticado.
     */
    @GetMapping
    public List<CuentaCorreoDto.Response> listar(@AuthenticationPrincipal UserDetails principal) {
        return cuentaCorreoService.listar(currentUserProvider.requireUserId(principal));
    }

    /**
     * Detalle de una cuenta de correo.
     */
    @GetMapping("/{id}")
    public CuentaCorreoDto.Response obtener(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails principal) {
        return cuentaCorreoService.obtener(id, currentUserProvider.requireUserId(principal));
    }

    /**
     * Alta manual de una cuenta de correo (sin flujo OAuth).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaCorreoDto.Response crear(@Valid @RequestBody CuentaCorreoDto.Create dto,
                                          @AuthenticationPrincipal UserDetails principal) {
        return cuentaCorreoService.crear(dto, currentUserProvider.requireUserId(principal));
    }

    /**
     * Edición de una cuenta de correo.
     */
    @PatchMapping("/{id}")
    public CuentaCorreoDto.Response actualizar(@PathVariable Long id,
                                               @Valid @RequestBody CuentaCorreoDto.Update dto,
                                               @AuthenticationPrincipal UserDetails principal) {
        return cuentaCorreoService.actualizar(id, currentUserProvider.requireUserId(principal), dto);
    }

    /**
     * Desconecta la cuenta de correo y revoca su token en Google.
     *
     * <p>Es una desconexión lógica: la fila se conserva (estado {@code DESCONECTADA})
     * para no perder el histórico de sincronizaciones.</p>
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desconectar(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails principal) {
        cuentaCorreoService.desconectar(id, currentUserProvider.requireUserId(principal));
    }

    /**
     * Inicia la vinculación OAuth2 de Gmail.
     *
     * <p>Devuelve la URL de Google a la que el frontend debe redirigir al navegador.
     * Además deja una cookie {@code gmail_link_state} que ata la solicitud al navegador
     * que la originó (protección contra CSRF).</p>
     */
    @PostMapping("/gmail")
    public ResponseEntity<CuentaCorreoDto.GmailAuthorize> autorizarGmail(
            @AuthenticationPrincipal UserDetails principal,
            HttpServletResponse response) {

        Long usuarioId = currentUserProvider.requireUserId(principal);
        CuentaCorreoDto.GmailAuthorize datos = gmailOAuthService.buildAuthorizationUrl(usuarioId);

        ResponseCookie cookie = ResponseCookie.from(STATE_COOKIE, datos.state())
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(stateTtlSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(datos);
    }

    /**
     * Callback de Google tras la vinculación.
     *
     * <p>Lo llama Google, no el usuario, así que va sin JWT: la protección la aporta
     * el {@code state} (validado contra el almacén en memoria y contra la cookie).
     * El resultado se comunica al frontend redirigiendo con {@code ?linked=ok} o
     * {@code ?linked=error}.</p>
     */
    @GetMapping("/gmail/callback")
    public void callbackGmail(@RequestParam("code") String code,
                              @RequestParam("state") String state,
                              @CookieValue(name = STATE_COOKIE, required = false) String cookieState,
                              HttpServletResponse response) throws IOException {

        try {
            GmailOAuthService.GmailToken token = gmailOAuthService.completarVinculacion(code, state, cookieState);
            cuentaCorreoService.registrarVinculacion(token.usuarioId(), token.email(), token.refreshToken());
            response.sendRedirect(redirectUri("ok", null));
        } catch (ApiException e) {
            log.warn("La vinculación con Gmail ha fallado: {}", e.getMessage());
            response.sendRedirect(redirectUri("error", e.getMessage()));
        }
    }

    /**
     * Construye la URL de redirección al frontend con el resultado de la vinculación.
     */
    private String redirectUri(String resultado, String motivo) {
        StringBuilder url = new StringBuilder(frontendBaseUrl)
                .append(FRONTEND_CUENTAS_PATH)
                .append("?linked=")
                .append(resultado);

        if (motivo != null && !motivo.isBlank()) {
            url.append("&reason=")
                    .append(URLEncoder.encode(motivo, StandardCharsets.UTF_8));
        }
        return url.toString();
    }
}