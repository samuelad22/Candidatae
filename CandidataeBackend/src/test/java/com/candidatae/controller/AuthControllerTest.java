package com.candidatae.controller;

import com.candidatae.security.JsonAccessDeniedHandler;
import com.candidatae.security.JsonAuthenticationEntryPoint;
import com.candidatae.config.SecurityConfig;
import com.candidatae.controller.AuthController;
import com.candidatae.dto.AuthDto;
import com.candidatae.domain.Usuario;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.JwtAuthenticationFilter;
import com.candidatae.security.JwtService;
import com.candidatae.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@ContextConfiguration(classes = {
        AuthController.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        AuthControllerTest.ProtectedEndpointTestController.class
})
@DisplayName("AuthController + Spring Security - integration tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("Test 1: valid DTO -> 201 Created with token and email in response")
        void validRegistrationReturnsCreated() throws Exception {
            when(authService.register(any(AuthDto.Register.class)))
                    .thenReturn(new AuthDto.Response("mock.jwt.token", "ana@correo.com"));

            String json = """
                    {
                      "email": "ana@correo.com",
                      "password": "password123",
                      "name": "Ana"
                    }
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                    .andExpect(jsonPath("$.email").value("ana@correo.com"))
                    .andExpect(jsonPath("$.password").doesNotExist());

            verify(authService).register(any(AuthDto.Register.class));
        }

        @Test
        @DisplayName("Test 2a: invalid email -> 400 Bad Request")
        void invalidEmailReturnsBadRequest() throws Exception {
            String json = """
                    {
                      "email": "invalid-email-format",
                      "password": "password123",
                      "name": "Ana"
                    }
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Test 2b: blank password -> 400 Bad Request")
        void blankPasswordReturnsBadRequest() throws Exception {
            String json = """
                    {
                      "email": "ana@correo.com",
                      "password": "",
                      "name": "Ana"
                    }
                    """;

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }

    @Nested
    @DisplayName("Protected routes (JwtAuthenticationFilter + SecurityConfig)")
    class ProtectedRoutes {

        @Test
        @DisplayName("Test 3: without Authorization header -> 401 Unauthorized")
        void withoutTokenReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/test/protected"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Test extra: invalid/malformed token -> 401 Unauthorized")
        void invalidTokenReturnsUnauthorized() throws Exception {
            when(jwtService.extractEmail("bad-token"))
                    .thenThrow(new io.jsonwebtoken.MalformedJwtException("Malformed token"));

            mockMvc.perform(get("/api/test/protected")
                            .header("Authorization", "Bearer bad-token"))
                    .andExpect(status().isUnauthorized());

            verify(usuarioRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("Test extra: valid token of active usuario -> 200 OK")
        void validTokenAllowsAccess() throws Exception {
            Usuario ana = Usuario.builder()
                    .id(1L)
                    .email("ana@correo.com")
                    .passwordHash("HASH")
                    .name("Ana")
                    .active(true)
                    .build();

            when(jwtService.extractEmail("valid-token")).thenReturn("ana@correo.com");
            when(usuarioRepository.findByEmail("ana@correo.com")).thenReturn(Optional.of(ana));
            when(jwtService.isTokenValid("valid-token", "ana@correo.com")).thenReturn(true);

            mockMvc.perform(get("/api/test/protected")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Hello ana@correo.com"));
        }

        @Test
        @DisplayName("Test extra: valid token but disabled usuario -> 401 Unauthorized")
        void disabledUserReturnsUnauthorized() throws Exception {
            Usuario banned = Usuario.builder()
                    .id(2L)
                    .email("banned@correo.com")
                    .passwordHash("HASH")
                    .active(false)
                    .build();

            when(jwtService.extractEmail("banned-token")).thenReturn("banned@correo.com");
            when(usuarioRepository.findByEmail("banned@correo.com")).thenReturn(Optional.of(banned));

            mockMvc.perform(get("/api/test/protected")
                            .header("Authorization", "Bearer banned-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @RestController
    static class ProtectedEndpointTestController {

        @GetMapping("/api/test/protected")
        String hello(@AuthenticationPrincipal UserDetails usuario) {
            return "Hello " + usuario.getUsername();
        }
    }
}
