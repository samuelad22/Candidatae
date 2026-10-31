package com.candidatae.controller;

import com.candidatae.config.SecurityConfig;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.TermsDto.TermsAcceptanceRequest;
import com.candidatae.dto.TermsDto.TermsStatusResponse;
import com.candidatae.exception.GlobalExceptionHandler;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.CurrentUserProvider;
import com.candidatae.security.JsonAccessDeniedHandler;
import com.candidatae.security.JsonAuthenticationEntryPoint;
import com.candidatae.security.JwtAuthenticationFilter;
import com.candidatae.security.JwtService;
import com.candidatae.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@ContextConfiguration(classes = {
        UserController.class,
        CurrentUserProvider.class,
        GlobalExceptionHandler.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@DisplayName("UserController - tests de la API REST (/api/users/terms y /api/users/me)")
class UserControllerTest {

    private static final Long USUARIO_ID = 1L;
    private static final String TOKEN = "Bearer token-valido";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
                .id(USUARIO_ID)
                .email("test@candidatae.com")
                .passwordHash("HASH")
                .name("Tester")
                .active(true)
                .build();

        when(jwtService.extractEmail("token-valido")).thenReturn("test@candidatae.com");
        when(jwtService.isTokenValid(eq("token-valido"), eq("test@candidatae.com"))).thenReturn(true);
        when(usuarioRepository.findByEmail("test@candidatae.com")).thenReturn(Optional.of(usuario));
        when(currentUserProvider.requireUserId(any(UserDetails.class))).thenReturn(USUARIO_ID);
    }

    @Test
    @DisplayName("GET /api/users/terms: devuelve 200 OK con el estado actual")
    void testGetTermsStatusDevuelve200() throws Exception {
        TermsStatusResponse response = new TermsStatusResponse(true, Instant.now(), "1.0");
        when(usuarioService.getTermsStatus(USUARIO_ID)).thenReturn(response);

        mockMvc.perform(get("/api/users/terms")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termsAccepted").value(true))
                .andExpect(jsonPath("$.termsVersion").value("1.0"));

        verify(usuarioService).getTermsStatus(USUARIO_ID);
    }

    @Test
    @DisplayName("POST /api/users/terms: registra consentimiento y devuelve 200 OK")
    void testUpdateTermsAcceptanceDevuelve200() throws Exception {
        TermsStatusResponse response = new TermsStatusResponse(true, Instant.now(), "1.0");
        when(usuarioService.updateTermsAcceptance(eq(USUARIO_ID), any(TermsAcceptanceRequest.class)))
                .thenReturn(response);

        String jsonBody = """
                {
                    "accepted": true,
                    "version": "1.0"
                }
                """;

        mockMvc.perform(post("/api/users/terms")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termsAccepted").value(true))
                .andExpect(jsonPath("$.termsVersion").value("1.0"));

        verify(usuarioService).updateTermsAcceptance(eq(USUARIO_ID), any(TermsAcceptanceRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/users/me: ejerce derecho al olvido y devuelve 204 No Content")
    void testDeleteAccountDevuelve204() throws Exception {
        doNothing().when(usuarioService).deleteAccount(USUARIO_ID);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNoContent());

        verify(usuarioService).deleteAccount(USUARIO_ID);
    }
}
