package com.candidatae.controller;

import com.candidatae.config.SecurityConfig;
import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.Modalidad;
import com.candidatae.domain.OrigenEvento;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CandidaturaDetailResponse;
import com.candidatae.dto.CandidaturaResponse;
import com.candidatae.dto.ContactoResponse;
import com.candidatae.dto.HistorialEstadoResponse;
import com.candidatae.dto.NotaResponse;
import com.candidatae.exception.ApiException;
import com.candidatae.exception.GlobalExceptionHandler;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.CurrentUserProvider;
import com.candidatae.security.JsonAccessDeniedHandler;
import com.candidatae.security.JsonAuthenticationEntryPoint;
import com.candidatae.security.JwtAuthenticationFilter;
import com.candidatae.security.JwtService;
import com.candidatae.service.CandidaturaService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CandidaturaController.class)
@ContextConfiguration(classes = {
        CandidaturaController.class,
        CurrentUserProvider.class,
        GlobalExceptionHandler.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@DisplayName("CandidaturaController - tests de la API REST")
class CandidaturaControllerTest {

    private static final Long USUARIO_ID = 1L;
    private static final String TOKEN = "Bearer token-valido";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidaturaService candidaturaService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
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

    private CandidaturaResponse crearEjemploResponse(Long id, String empresa, String puesto, EstadoCandidatura estado) {
        return new CandidaturaResponse(
                id,
                10L,
                empresa,
                20L,
                "LINKEDIN",
                "LinkedIn",
                puesto,
                "Madrid",
                Modalidad.REMOTO,
                "40.000€",
                "https://example.com/job",
                LocalDate.of(2026, 10, 20),
                estado,
                Instant.parse("2026-10-24T12:00:00Z"),
                "clave-dedup-1"
        );
    }

    @Test
    @DisplayName("GET /api/applications: devuelve 200 con la lista de candidaturas y respeta los filtros")
    void testListarCandidaturasConFiltros() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        when(candidaturaService.listar("LINKEDIN", EstadoCandidatura.ENVIADA, "Google", from, to, "Backend"))
                .thenReturn(List.of(crearEjemploResponse(1L, "Google", "Backend Engineer", EstadoCandidatura.ENVIADA)));

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", TOKEN)
                        .param("platform", "LINKEDIN")
                        .param("status", "ENVIADA")
                        .param("company", "Google")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("search", "Backend"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].empresaNombre").value("Google"))
                .andExpect(jsonPath("$[0].puesto").value("Backend Engineer"))
                .andExpect(jsonPath("$[0].estadoActual").value("ENVIADA"))
                .andExpect(jsonPath("$[0].plataformaCodigo").value("LINKEDIN"));

        verify(candidaturaService).listar("LINKEDIN", EstadoCandidatura.ENVIADA, "Google", from, to, "Backend");
    }

    @Test
    @DisplayName("GET /api/applications: devuelve 401 si no está autenticado")
    void testListarSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    private CandidaturaDetailResponse crearEjemploDetailResponse(Long id, String empresa, String puesto, EstadoCandidatura estado) {
        return new CandidaturaDetailResponse(
                id,
                10L,
                empresa,
                20L,
                "LINKEDIN",
                "LinkedIn",
                puesto,
                "Madrid",
                Modalidad.REMOTO,
                "40.000€",
                "https://example.com/job",
                LocalDate.of(2026, 10, 20),
                estado,
                Instant.parse("2026-10-24T12:00:00Z"),
                "clave-dedup-1",
                List.of(new HistorialEstadoResponse(
                        1L,
                        null,
                        EstadoCandidatura.ENVIADA,
                        Instant.parse("2026-10-20T10:00:00Z"),
                        OrigenEvento.MANUAL,
                        null
                )),
                List.of(new NotaResponse(
                        10L,
                        "Nota importante",
                        Instant.parse("2026-10-21T10:00:00Z")
                )),
                List.of(new ContactoResponse(
                        20L,
                        "Ana Recruiter",
                        "ana@recruiter.com",
                        "Talent Acquisition"
                ))
        );
    }

    @Test
    @DisplayName("GET /api/applications/{id}: devuelve 200 con el detalle completo de la candidatura")
    void testObtenerDetalleCandidatura() throws Exception {
        when(candidaturaService.obtenerDetalle(100L))
                .thenReturn(crearEjemploDetailResponse(100L, "Google", "Backend Engineer", EstadoCandidatura.ENVIADA));

        mockMvc.perform(get("/api/applications/100")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.empresaNombre").value("Google"))
                .andExpect(jsonPath("$.puesto").value("Backend Engineer"))
                .andExpect(jsonPath("$.historial.length()").value(1))
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("ENVIADA"))
                .andExpect(jsonPath("$.notas.length()").value(1))
                .andExpect(jsonPath("$.notas[0].texto").value("Nota importante"))
                .andExpect(jsonPath("$.contactos.length()").value(1))
                .andExpect(jsonPath("$.contactos[0].nombre").value("Ana Recruiter"));

        verify(candidaturaService).obtenerDetalle(100L);
    }

    @Test
    @DisplayName("GET /api/applications/{id}: devuelve 404 cuando la candidatura no existe o no pertenece al usuario")
    void testObtenerDetalleNoEncontradaDevuelve404() throws Exception {
        when(candidaturaService.obtenerDetalle(999L))
                .thenThrow(ApiException.notFound("Candidatura no encontrada"));

        mockMvc.perform(get("/api/applications/999")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Candidatura no encontrada"));

        verify(candidaturaService).obtenerDetalle(999L);
    }

    @Test
    @DisplayName("GET /api/applications/{id}: devuelve 401 si no está autenticado")
    void testObtenerDetalleSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/applications/100"))
                .andExpect(status().isUnauthorized());
    }
}
