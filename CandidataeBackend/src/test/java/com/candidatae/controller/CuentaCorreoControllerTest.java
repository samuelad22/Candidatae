package com.candidatae.controller;

import com.candidatae.exception.GlobalExceptionHandler;
import com.candidatae.security.CurrentUserProvider;
import com.candidatae.security.JsonAccessDeniedHandler;
import com.candidatae.security.JsonAuthenticationEntryPoint;
import com.candidatae.controller.CuentaCorreoController;
import com.candidatae.dto.CuentaCorreoDto;
import com.candidatae.dto.EmailSyncDto;
import com.candidatae.service.EmailSyncService;
import com.candidatae.exception.ApiException;
import com.candidatae.service.CuentaCorreoService;
import com.candidatae.integration.gmail.GmailOAuthService;
import com.candidatae.integration.outlook.OutlookOAuthService;
import com.candidatae.config.SecurityConfig;
import com.candidatae.domain.Usuario;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.JwtAuthenticationFilter;
import com.candidatae.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CuentaCorreoController.class)
@ContextConfiguration(classes = {
        CuentaCorreoController.class,
        CurrentUserProvider.class,
        GlobalExceptionHandler.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@DisplayName("CuentaCorreoController - tests de la API REST")
class CuentaCorreoControllerTest {

    private static final Long USUARIO_ID = 1L;
    private static final String TOKEN = "Bearer token-valido";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CuentaCorreoService cuentaCorreoService;

    @MockitoBean
    private GmailOAuthService gmailOAuthService;

    @MockitoBean
    private OutlookOAuthService outlookOAuthService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmailSyncService emailSyncService;

    @BeforeEach
    void setUp() {
        // Autenticación válida por defecto (salvo en los tests que la cambian).
        Usuario ana = Usuario.builder()
                .id(USUARIO_ID)
                .email("ana@correo.com")
                .passwordHash("HASH")
                .name("Ana")
                .active(true)
                .build();

        when(jwtService.extractEmail("token-valido")).thenReturn("ana@correo.com");
        when(jwtService.isTokenValid(eq("token-valido"), eq("ana@correo.com"))).thenReturn(true);
        when(usuarioRepository.findByEmail("ana@correo.com")).thenReturn(Optional.of(ana));
        when(currentUserProvider.requireUserId(any(UserDetails.class))).thenReturn(USUARIO_ID);
    }

    private CuentaCorreoDto.Response cuenta(Long id, String email, String estado) {
        return new CuentaCorreoDto.Response(id, "GMAIL", email, estado,
                Instant.parse("2026-10-20T10:15:00Z"), null, "ACTIVA".equals(estado));
    }

    @Nested
    @DisplayName("GET /api/email-accounts")
    class Listar {

        @Test
        @DisplayName("Test 1: devuelve 200 con la lista de cuentas del usuario")
        void devuelve200ConLaLista() throws Exception {
            when(cuentaCorreoService.listar(USUARIO_ID))
                    .thenReturn(List.of(cuenta(1L, "ana@gmail.com", "ACTIVA")));

            mockMvc.perform(get("/api/email-accounts").header("Authorization", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].emailCuenta").value("ana@gmail.com"))
                    .andExpect(jsonPath("$[0].estadoConexion").value("ACTIVA"))
                    .andExpect(jsonPath("$[0].activa").value(true));
        }

        @Test
        @DisplayName("Test 2: sin token -> 401 con el formato de error de la API")
        void sinTokenDevuelve401() throws Exception {
            mockMvc.perform(get("/api/email-accounts"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").exists());

            verifyNoInteractions(cuentaCorreoService);
        }

        @Test
        @DisplayName("Test 3: la respuesta NUNCA incluye el refresh token")
        void laRespuestaNuncaIncluyeElToken() throws Exception {
            when(cuentaCorreoService.listar(USUARIO_ID))
                    .thenReturn(List.of(cuenta(1L, "ana@gmail.com", "ACTIVA")));

            mockMvc.perform(get("/api/email-accounts").header("Authorization", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].refreshToken").doesNotExist())
                    .andExpect(jsonPath("$[0].refreshTokenCifrado").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/email-accounts/{id}")
    class Obtener {

        @Test
        @DisplayName("Test 4: devuelve 200 con el detalle")
        void devuelve200ConElDetalle() throws Exception {
            when(cuentaCorreoService.obtener(1L, USUARIO_ID))
                    .thenReturn(cuenta(1L, "ana@gmail.com", "PENDIENTE"));

            mockMvc.perform(get("/api/email-accounts/1").header("Authorization", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emailCuenta").value("ana@gmail.com"));
        }

        @Test
        @DisplayName("Test 5: cuenta ajena -> 404")
        void cuentaAjenaDevuelve404() throws Exception {
            when(cuentaCorreoService.obtener(1L, USUARIO_ID))
                    .thenThrow(ApiException.notFound(
                            "Cuenta de correo no encontrada"));

            mockMvc.perform(get("/api/email-accounts/1").header("Authorization", TOKEN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Cuenta de correo no encontrada"));
        }
    }

    @Nested
    @DisplayName("POST /api/email-accounts")
    class Crear {

        @Test
        @DisplayName("Test 6: alta válida -> 201")
        void altaValidaDevuelve201() throws Exception {
            when(cuentaCorreoService.crear(any(), eq(USUARIO_ID)))
                    .thenReturn(cuenta(1L, "ana@gmail.com", "PENDIENTE"));

            String json = """
                    { "emailCuenta": "ana@gmail.com", "proveedor": "GMAIL" }
                    """;

            mockMvc.perform(post("/api/email-accounts")
                            .header("Authorization", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.estadoConexion").value("PENDIENTE"));
        }

        @Test
        @DisplayName("Test 7: email inválido -> 400 con el detalle del campo")
        void emailInvalidoDevuelve400() throws Exception {
            String json = """
                    { "emailCuenta": "no-es-email", "proveedor": "GMAIL" }
                    """;

            mockMvc.perform(post("/api/email-accounts")
                            .header("Authorization", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.emailCuenta").exists());

            verifyNoInteractions(cuentaCorreoService);
        }

        @Test
        @DisplayName("Test 8: cuenta duplicada -> 409")
        void cuentaDuplicadaDevuelve409() throws Exception {
            when(cuentaCorreoService.crear(any(), eq(USUARIO_ID)))
                    .thenThrow(ApiException.conflict(
                            "Ya tienes registrada la cuenta de correo ana@gmail.com"));

            String json = """
                    { "emailCuenta": "ana@gmail.com", "proveedor": "GMAIL" }
                    """;

            mockMvc.perform(post("/api/email-accounts")
                            .header("Authorization", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Ya tienes registrada la cuenta de correo ana@gmail.com"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/email-accounts/{id}")
    class Actualizar {

        @Test
        @DisplayName("Test 9: edición válida -> 200")
        void edicionValidaDevuelve200() throws Exception {
            when(cuentaCorreoService.actualizar(eq(1L), eq(USUARIO_ID), any()))
                    .thenReturn(cuenta(1L, "ana@gmail.com", "EXPIRADA"));

            String json = """
                    { "estadoConexion": "EXPIRADA", "resetCursor": true }
                    """;

            mockMvc.perform(patch("/api/email-accounts/1")
                            .header("Authorization", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estadoConexion").value("EXPIRADA"));
        }

        @Test
        @DisplayName("Test 10: estado no permitido -> 400")
        void estadoNoPermitidoDevuelve400() throws Exception {
            when(cuentaCorreoService.actualizar(eq(1L), eq(USUARIO_ID), any()))
                    .thenThrow(ApiException.badRequest(
                            "Solo se puede marcar la cuenta como PENDIENTE o EXPIRADA."));

            String json = """
                    { "estadoConexion": "ACTIVA" }
                    """;

            mockMvc.perform(patch("/api/email-accounts/1")
                            .header("Authorization", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/email-accounts/{id}")
    class Desconectar {

        @Test
        @DisplayName("Test 11: desconexión correcta -> 204 sin cuerpo")
        void desconexionCorrectaDevuelve204() throws Exception {
            mockMvc.perform(delete("/api/email-accounts/1").header("Authorization", TOKEN))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(cuentaCorreoService).desconectar(1L, USUARIO_ID);
        }

        @Test
        @DisplayName("Test 12: cuenta ajena -> 404 y no se borra nada")
        void cuentaAjenaDevuelve404YNoSeTocaNada() throws Exception {
            org.mockito.Mockito.doThrow(ApiException.notFound(
                    "Cuenta de correo no encontrada"))
                    .when(cuentaCorreoService).desconectar(anyLong(), anyLong());

            mockMvc.perform(delete("/api/email-accounts/1").header("Authorization", TOKEN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Cuenta de correo no encontrada"));

            // La llamada sí llega al servicio (es el quien decide), pero no borra nada.
            verify(cuentaCorreoService).desconectar(1L, USUARIO_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/email-accounts/gmail")
    class AutorizarGmail {

        @Test
        @DisplayName("Test 13: devuelve la URL de Google y deja la cookie anti-CSRF")
        void devuelveLaUrlYLaCookie() throws Exception {
            when(gmailOAuthService.buildAuthorizationUrl(USUARIO_ID))
                    .thenReturn(new CuentaCorreoDto.GmailAuthorize(
                            "https://accounts.google.com/o/oauth2/v2/auth?client_id=abc&state=xyz",
                            "xyz"));

            mockMvc.perform(post("/api/email-accounts/gmail").header("Authorization", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorizationUrl").value(
                            "https://accounts.google.com/o/oauth2/v2/auth?client_id=abc&state=xyz"))
                    .andExpect(jsonPath("$.state").value("xyz"))
                    .andExpect(cookie().exists("gmail_link_state"))
                    .andExpect(cookie().httpOnly("gmail_link_state", true))
                    .andExpect(cookie().secure("gmail_link_state", false));
        }
    }

    @Nested
    @DisplayName("GET /api/email-accounts/gmail/callback")
    class CallbackGmail {

        @Test
        @DisplayName("Test 14: vinculación correcta -> 302 al frontend con linked=ok")
        void vinculacionCorrectaRedirigeConOk() throws Exception {
            when(gmailOAuthService.completarVinculacion("code-123", "state-xyz", "state-xyz"))
                    .thenReturn(new GmailOAuthService.GmailToken(USUARIO_ID, "ana@gmail.com", "refresh-token"));

            mockMvc.perform(get("/api/email-accounts/gmail/callback")
                            .param("code", "code-123")
                            .param("state", "state-xyz")
                            .cookie(new jakarta.servlet.http.Cookie("gmail_link_state", "state-xyz")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost:5173/cuentas-correo?linked=ok"));

            verify(cuentaCorreoService).registrarVinculacion(USUARIO_ID, "ana@gmail.com", "refresh-token");
        }

        @Test
        @DisplayName("Test 15: error de Google -> 302 al frontend con linked=error")
        void errorDeGoogleRedirigeConError() throws Exception {
            when(gmailOAuthService.completarVinculacion(eq("code-123"), eq("state-xyz"), any()))
                    .thenThrow(ApiException.oauth(
                            "Google ha rechazado la vinculación."));

            mockMvc.perform(get("/api/email-accounts/gmail/callback")
                            .param("code", "code-123")
                            .param("state", "state-xyz")
                            .cookie(new jakarta.servlet.http.Cookie("gmail_link_state", "state-xyz")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(result -> {
                        String url = result.getResponse().getRedirectedUrl();
                        assertNotNull(url, "No se ha realizado ninguna redirección");
                        assertTrue(url.startsWith("http://localhost:5173/cuentas-correo?linked=error"),
                                "Redirección inesperada: " + url);
                    });

            verify(cuentaCorreoService, never()).registrarVinculacion(anyLong(), any(), any());
        }

        @Test
        @DisplayName("Test 16: sin los parametros de Google -> 400 (no un 500)")
        void sinParametrosDeGoogleResponde400() throws Exception {
            mockMvc.perform(get("/api/email-accounts/gmail/callback"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Falta el parámetro obligatorio 'code'"));
        }

        @Test
        @DisplayName("Test 17: cancelacion en Google con param error -> 302 a linked=error y limpia cookie")
        void cancelacionEnGoogleRedirigeConError() throws Exception {
            mockMvc.perform(get("/api/email-accounts/gmail/callback")
                            .param("error", "access_denied")
                            .param("state", "state-xyz"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(result -> {
                        String url = result.getResponse().getRedirectedUrl();
                        assertNotNull(url);
                        assertTrue(url.contains("linked=error"));
                        assertTrue(url.contains("access_denied"));
                    })
                    .andExpect(cookie().maxAge("gmail_link_state", 0));
        }
    }

    @Nested
    @DisplayName("POST /api/email-accounts/outlook")
    class AutorizarOutlook {

        @Test
        @DisplayName("Test Outlook 1: devuelve la URL de Microsoft y deja la cookie anti-CSRF")
        void devuelveLaUrlYLaCookieOutlook() throws Exception {
            when(outlookOAuthService.buildAuthorizationUrl(USUARIO_ID))
                    .thenReturn(new CuentaCorreoDto.OutlookAuthorize(
                            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=ms-123&state=xyz",
                            "xyz"));

            mockMvc.perform(post("/api/email-accounts/outlook").header("Authorization", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorizationUrl").value(
                            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=ms-123&state=xyz"))
                    .andExpect(jsonPath("$.state").value("xyz"))
                    .andExpect(cookie().exists("outlook_link_state"))
                    .andExpect(cookie().httpOnly("outlook_link_state", true))
                    .andExpect(cookie().secure("outlook_link_state", false));
        }
    }

    @Nested
    @DisplayName("GET /api/email-accounts/outlook/callback")
    class CallbackOutlook {

        @Test
        @DisplayName("Test Outlook 2: vinculación correcta -> 302 al frontend con linked=ok")
        void vinculacionOutlookCorrectaRedirigeConOk() throws Exception {
            when(outlookOAuthService.completarVinculacion("code-ms", "state-xyz", "state-xyz"))
                    .thenReturn(new OutlookOAuthService.OutlookToken(USUARIO_ID, "ana@outlook.com", "refresh-token-ms"));

            mockMvc.perform(get("/api/email-accounts/outlook/callback")
                            .param("code", "code-ms")
                            .param("state", "state-xyz")
                            .cookie(new jakarta.servlet.http.Cookie("outlook_link_state", "state-xyz")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost:5173/cuentas-correo?linked=ok"));

            verify(cuentaCorreoService).registrarVinculacion(
                    USUARIO_ID, "ana@outlook.com", "refresh-token-ms", com.candidatae.domain.ProveedorCorreo.OUTLOOK);
        }
    }

    @Nested
    @DisplayName("POST /api/email-accounts/imap & POST /api/email-accounts/{id}/sync")
    class ImapAndSync {

        @Test
        @DisplayName("Test 18: POST /api/email-accounts/imap connects account with App Password -> 201")
        void connectImapReturns201() throws Exception {
            when(emailSyncService.connectImapAccount(eq(USUARIO_ID), any()))
                    .thenReturn(cuenta(1L, "ana@gmail.com", "ACTIVA"));

            String json = """
                    { "email": "ana@gmail.com", "appPassword": "abcd efgh ijkl mnop", "provider": "GMAIL" }
                    """;

            mockMvc.perform(post("/api/email-accounts/imap")
                            .header("Authorization", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.emailCuenta").value("ana@gmail.com"))
                    .andExpect(jsonPath("$.estadoConexion").value("ACTIVA"));
        }

        @Test
        @DisplayName("Test 19: POST /api/email-accounts/{id}/sync triggers manual sync -> 200")
        void syncEmailAccountReturns200() throws Exception {
            EmailSyncDto.SyncResultDto syncResult = new EmailSyncDto.SyncResultDto(
                    1L, 10, 2, 1, Instant.parse("2026-10-27T12:00:00Z"), "SUCCESS", List.of()
            );
            when(emailSyncService.syncAccount(1L, USUARIO_ID)).thenReturn(syncResult);

            mockMvc.perform(post("/api/email-accounts/1/sync").header("Authorization", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(1))
                    .andExpect(jsonPath("$.emailsScanned").value(10))
                    .andExpect(jsonPath("$.candidaciesCreated").value(2))
                    .andExpect(jsonPath("$.candidaciesUpdated").value(1))
                    .andExpect(jsonPath("$.syncStatus").value("SUCCESS"));
        }

        @Test
        @DisplayName("Test 20: POST /api/email-accounts/sync triggers sync across ALL user accounts -> 200")
        void syncAllEmailAccountsReturns200() throws Exception {
            EmailSyncDto.MultiAccountSyncResultDto multiResult = new EmailSyncDto.MultiAccountSyncResultDto(
                    2, 25, 4, 3, Instant.parse("2026-10-27T12:00:00Z"), List.of()
            );
            when(emailSyncService.syncAllUserAccounts(USUARIO_ID)).thenReturn(multiResult);

            mockMvc.perform(post("/api/email-accounts/sync").header("Authorization", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountsSynced").value(2))
                    .andExpect(jsonPath("$.totalEmailsScanned").value(25))
                    .andExpect(jsonPath("$.totalCandidaciesCreated").value(4))
                    .andExpect(jsonPath("$.totalCandidaciesUpdated").value(3));
        }
    }
}