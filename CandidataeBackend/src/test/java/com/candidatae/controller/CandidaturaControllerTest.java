package com.candidatae.controller;

import com.candidatae.config.SecurityConfig;
import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.Modalidad;
import com.candidatae.domain.OrigenEvento;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CandidaturaCreateRequest;
import com.candidatae.dto.CandidaturaDetailResponse;
import com.candidatae.dto.CandidaturaResponse;
import com.candidatae.dto.CandidaturaUpdateRequest;
import com.candidatae.dto.PageResponse;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @DisplayName("POST /api/applications: da de alta una candidatura y devuelve 201 Created")
    void testCrearCandidaturaExitosoDevuelve201() throws Exception {
        CandidaturaResponse response = crearEjemploResponse(100L, "Google España S.L.", "Backend Engineer", EstadoCandidatura.ENVIADA);
        when(candidaturaService.crear(any(CandidaturaCreateRequest.class))).thenReturn(response);

        String jsonBody = """
                {
                    "empresa": "Google España S.L.",
                    "puesto": "Backend Engineer",
                    "plataformaCodigo": "GLASSDOOR",
                    "ubicacion": "Madrid",
                    "modalidad": "REMOTO",
                    "salarioTexto": "45.000€",
                    "enlaceOferta": "https://glassdoor.com/job/123",
                    "fechaSolicitud": "2026-10-25"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.empresaNombre").value("Google España S.L."))
                .andExpect(jsonPath("$.puesto").value("Backend Engineer"))
                .andExpect(jsonPath("$.estadoActual").value("ENVIADA"));

        verify(candidaturaService).crear(any(CandidaturaCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/applications: devuelve 400 Bad Request si faltan campos obligatorios como puesto o empresa")
    void testCrearCandidaturaCamposObligatoriosFaltantesDevuelve400() throws Exception {
        String jsonInvalido = """
                {
                    "empresa": "",
                    "puesto": "   ",
                    "fechaSolicitud": null
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.empresa").exists())
                .andExpect(jsonPath("$.fieldErrors.puesto").exists());
    }

    @Test
    @DisplayName("POST /api/applications: devuelve 409 Conflict si la candidatura ya existe")
    void testCrearCandidaturaDuplicadaDevuelve409() throws Exception {
        when(candidaturaService.crear(any(CandidaturaCreateRequest.class)))
                .thenThrow(ApiException.conflict("Ya tienes registrada una candidatura para este puesto, empresa y plataforma"));

        String jsonBody = """
                {
                    "empresa": "Google",
                    "puesto": "Backend Engineer",
                    "fechaSolicitud": "2026-10-25"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya tienes registrada una candidatura para este puesto, empresa y plataforma"));
    }

    @Test
    @DisplayName("POST /api/applications: devuelve 401 si no está autenticado")
    void testCrearSinAutenticacionDevuelve401() throws Exception {
        String jsonBody = """
                {
                    "empresa": "Google",
                    "puesto": "Backend",
                    "fechaSolicitud": "2026-10-25"
                }
                """;

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/applications: devuelve 200 con la lista de candidaturas paginada y respeta los filtros")
    void testListarCandidaturasConFiltros() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        PageResponse<CandidaturaResponse> pageResponse = new PageResponse<>(
                List.of(crearEjemploResponse(1L, "Google", "Backend Engineer", EstadoCandidatura.ENVIADA)),
                0,
                20,
                1L,
                1,
                true
        );

        when(candidaturaService.listar("LINKEDIN", "ENVIADA", "Google", from, to, "Backend", 0, 20))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", TOKEN)
                        .param("platform", "LINKEDIN")
                        .param("status", "ENVIADA")
                        .param("company", "Google")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("search", "Backend")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].empresaNombre").value("Google"))
                .andExpect(jsonPath("$.content[0].puesto").value("Backend Engineer"))
                .andExpect(jsonPath("$.content[0].estadoActual").value("ENVIADA"))
                .andExpect(jsonPath("$.content[0].plataformaCodigo").value("LINKEDIN"));

        verify(candidaturaService).listar("LINKEDIN", "ENVIADA", "Google", from, to, "Backend", 0, 20);
    }

    @Test
    @DisplayName("GET /api/applications: devuelve 400 Bad Request si el filtro status es erróneo")
    void testListarStatusInvalidoDevuelve400() throws Exception {
        when(candidaturaService.listar(null, "ENVIAD", null, null, null, null, 0, 20))
                .thenThrow(ApiException.badRequest("Valor 'ENVIAD' no válido para 'status'. Valores permitidos: [ENVIADA, EN_PROCESO, ENTREVISTA, OFERTA, RECHAZADA, DESCARTADA]"));

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", TOKEN)
                        .param("status", "ENVIAD"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Valor 'ENVIAD' no válido para 'status'")));
    }

    @Test
    @DisplayName("GET /api/applications: devuelve 400 Bad Request si la paginación es inválida")
    void testListarPaginacionInvalidaDevuelve400() throws Exception {
        when(candidaturaService.listar(null, null, null, null, null, null, -1, 20))
                .thenThrow(ApiException.badRequest("El número de página no puede ser negativo"));

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", TOKEN)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El número de página no puede ser negativo"));
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

    @Test
    @DisplayName("PUT /api/applications/{id}: actualiza correctamente y devuelve 200 con la candidatura")
    void testActualizarCandidaturaExitoso() throws Exception {
        when(candidaturaService.actualizar(eq(100L), any(CandidaturaUpdateRequest.class)))
                .thenReturn(crearEjemploResponse(100L, "Google", "Lead Engineer", EstadoCandidatura.ENTREVISTA));

        String jsonBody = """
                {
                    "puesto": "Lead Engineer",
                    "estadoActual": "ENTREVISTA"
                }
                """;

        mockMvc.perform(put("/api/applications/100")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.empresaNombre").value("Google"))
                .andExpect(jsonPath("$.puesto").value("Lead Engineer"))
                .andExpect(jsonPath("$.estadoActual").value("ENTREVISTA"));

        verify(candidaturaService).actualizar(eq(100L), any(CandidaturaUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/applications/{id}: devuelve 400 con lista de valores permitidos si el enum es inválido")
    void testActualizarEnumInvalidoDevuelve400ConValoresPermitidos() throws Exception {
        String jsonBody = """
                {
                    "estadoActual": "envrevista"
                }
                """;

        mockMvc.perform(put("/api/applications/100")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Valor 'envrevista' no válido para el campo 'estadoActual'")))
                .andExpect(jsonPath("$.message", containsString("Valores permitidos: [ENVIADA, EN_PROCESO, ENTREVISTA, OFERTA, RECHAZADA, DESCARTADA]")));
    }

    @Test
    @DisplayName("PUT /api/applications/{id}: acepta enum en minúsculas y devuelve 200 OK")
    void testActualizarEnumMinusculasExitoso() throws Exception {
        when(candidaturaService.actualizar(eq(100L), any(CandidaturaUpdateRequest.class)))
                .thenReturn(crearEjemploResponse(100L, "Google", "Lead Engineer", EstadoCandidatura.ENTREVISTA));

        String jsonBody = """
                {
                    "estadoActual": "entrevista"
                }
                """;

        mockMvc.perform(put("/api/applications/100")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estadoActual").value("ENTREVISTA"));

        verify(candidaturaService).actualizar(eq(100L), any(CandidaturaUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/applications/{id}: devuelve 409 Conflict si la actualización genera duplicidad")
    void testActualizarDuplicadoDevuelve409() throws Exception {
        when(candidaturaService.actualizar(eq(100L), any(CandidaturaUpdateRequest.class)))
                .thenThrow(ApiException.conflict("Ya tienes registrada otra candidatura para este puesto, empresa y plataforma"));

        String jsonBody = """
                {
                    "puesto": "Senior Lead Engineer"
                }
                """;

        mockMvc.perform(put("/api/applications/100")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya tienes registrada otra candidatura para este puesto, empresa y plataforma"));
    }

    @Test
    @DisplayName("PUT /api/applications/{id}: devuelve 404 si la candidatura no existe o no pertenece al usuario")
    void testActualizarNoEncontradaDevuelve404() throws Exception {
        when(candidaturaService.actualizar(eq(999L), any(CandidaturaUpdateRequest.class)))
                .thenThrow(ApiException.notFound("Candidatura no encontrada"));

        String jsonBody = """
                {
                    "estadoActual": "ENTREVISTA"
                }
                """;

        mockMvc.perform(put("/api/applications/999")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Candidatura no encontrada"));

        verify(candidaturaService).actualizar(eq(999L), any(CandidaturaUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/applications/{id}: devuelve 401 si no está autenticado")
    void testActualizarSinAutenticacionDevuelve401() throws Exception {
        String jsonBody = """
                {
                    "estadoActual": "ENTREVISTA"
                }
                """;

        mockMvc.perform(put("/api/applications/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/applications/{id}: elimina la candidatura y devuelve 204 No Content")
    void testEliminarCandidaturaExitoso() throws Exception {
        doNothing().when(candidaturaService).eliminar(100L);

        mockMvc.perform(delete("/api/applications/100")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNoContent());

        verify(candidaturaService).eliminar(100L);
    }

    @Test
    @DisplayName("DELETE /api/applications/{id}: devuelve 404 si la candidatura no existe o no pertenece al usuario")
    void testEliminarNoEncontradaDevuelve404() throws Exception {
        doThrow(ApiException.notFound("Candidatura no encontrada")).when(candidaturaService).eliminar(999L);

        mockMvc.perform(delete("/api/applications/999")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Candidatura no encontrada"));

        verify(candidaturaService).eliminar(999L);
    }

    @Test
    @DisplayName("DELETE /api/applications/{id}: devuelve 401 si no está autenticado")
    void testEliminarSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(delete("/api/applications/100"))
                .andExpect(status().isUnauthorized());
    }
}
