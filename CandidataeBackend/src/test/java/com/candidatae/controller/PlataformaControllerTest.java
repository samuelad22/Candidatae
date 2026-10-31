package com.candidatae.controller;

import com.candidatae.config.SecurityConfig;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.PlataformaDto.PlataformaCreateRequest;
import com.candidatae.dto.PlataformaDto.PlataformaResponse;
import com.candidatae.dto.PlataformaDto.PlataformaUpdateRequest;
import com.candidatae.exception.ApiException;
import com.candidatae.exception.GlobalExceptionHandler;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.CurrentUserProvider;
import com.candidatae.security.JsonAccessDeniedHandler;
import com.candidatae.security.JsonAuthenticationEntryPoint;
import com.candidatae.security.JwtAuthenticationFilter;
import com.candidatae.security.JwtService;
import com.candidatae.service.PlataformaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

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

@WebMvcTest(controllers = PlataformaController.class)
@ContextConfiguration(classes = {
        PlataformaController.class,
        CurrentUserProvider.class,
        GlobalExceptionHandler.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@DisplayName("PlataformaController - tests de la API REST (/api/platforms)")
class PlataformaControllerTest {

    private static final Long USUARIO_ID = 1L;
    private static final String TOKEN = "Bearer token-valido";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlataformaService plataformaService;

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
    @DisplayName("GET /api/platforms: devuelve 200 OK con el listado alfabético")
    void testListarPlataformasSinBusquedaDevuelve200() throws Exception {
        List<PlataformaResponse> plataformas = List.of(
                new PlataformaResponse(1L, "INDEED", "Indeed", null, null),
                new PlataformaResponse(2L, "LINKEDIN", "LinkedIn", null, null)
        );
        when(plataformaService.listar(null)).thenReturn(plataformas);

        mockMvc.perform(get("/api/platforms")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("INDEED"))
                .andExpect(jsonPath("$[0].nombre").value("Indeed"))
                .andExpect(jsonPath("$[1].codigo").value("LINKEDIN"));

        verify(plataformaService).listar(null);
    }

    @Test
    @DisplayName("GET /api/plataformas: alias en castellano devuelve 200 OK")
    void testListarPlataformasAliasCastellanoDevuelve200() throws Exception {
        when(plataformaService.listar(null)).thenReturn(List.of(
                new PlataformaResponse(1L, "INFOJOBS", "InfoJobs", null, null)
        ));

        mockMvc.perform(get("/api/plataformas")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("INFOJOBS"));
    }

    @Test
    @DisplayName("GET /api/platforms/{id}: devuelve 200 OK con los datos de la plataforma")
    void testObtenerPorIdExitosoDevuelve200() throws Exception {
        PlataformaResponse plataforma = new PlataformaResponse(2L, "GLASSDOOR", "Glassdoor", 1L, "Indeed");
        when(plataformaService.obtenerPorId(2L)).thenReturn(plataforma);

        mockMvc.perform(get("/api/platforms/2")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.codigo").value("GLASSDOOR"))
                .andExpect(jsonPath("$.nombre").value("Glassdoor"))
                .andExpect(jsonPath("$.plataformaOrigenId").value(1))
                .andExpect(jsonPath("$.plataformaOrigenNombre").value("Indeed"));

        verify(plataformaService).obtenerPorId(2L);
    }

    @Test
    @DisplayName("GET /api/platforms/{id}: devuelve 404 Not Found si no existe")
    void testObtenerPorIdNoExisteDevuelve404() throws Exception {
        when(plataformaService.obtenerPorId(999L)).thenThrow(ApiException.notFound("Plataforma no encontrada"));

        mockMvc.perform(get("/api/platforms/999")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Plataforma no encontrada"));
    }

    @Test
    @DisplayName("GET /api/platforms/code/{codigo}: devuelve 200 OK")
    void testObtenerPorCodigoDevuelve200() throws Exception {
        PlataformaResponse plataforma = new PlataformaResponse(1L, "LINKEDIN", "LinkedIn", null, null);
        when(plataformaService.obtenerPorCodigo("LINKEDIN")).thenReturn(plataforma);

        mockMvc.perform(get("/api/platforms/code/LINKEDIN")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("LINKEDIN"))
                .andExpect(jsonPath("$.nombre").value("LinkedIn"));

        verify(plataformaService).obtenerPorCodigo("LINKEDIN");
    }

    @Test
    @DisplayName("POST /api/platforms: crea una nueva plataforma y devuelve 201 Created")
    void testCrearPlataformaExitosoDevuelve201() throws Exception {
        PlataformaResponse creada = new PlataformaResponse(5L, "MANFRED", "Manfred", null, null);
        when(plataformaService.crear(any(PlataformaCreateRequest.class))).thenReturn(creada);

        String jsonBody = """
                {
                    "codigo": "MANFRED",
                    "nombre": "Manfred"
                }
                """;

        mockMvc.perform(post("/api/platforms")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.codigo").value("MANFRED"))
                .andExpect(jsonPath("$.nombre").value("Manfred"));

        verify(plataformaService).crear(any(PlataformaCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/platforms: devuelve 400 Bad Request si los campos obligatorios están en blanco")
    void testCrearPlataformaCamposVaciosDevuelve400() throws Exception {
        String jsonBody = """
                {
                    "codigo": "   ",
                    "nombre": ""
                }
                """;

        mockMvc.perform(post("/api/platforms")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/platforms: devuelve 409 Conflict si el código ya existe")
    void testCrearPlataformaCodigoDuplicadoDevuelve409() throws Exception {
        when(plataformaService.crear(any(PlataformaCreateRequest.class)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Ya existe una plataforma con ese código"));

        String jsonBody = """
                {
                    "codigo": "LINKEDIN",
                    "nombre": "LinkedIn"
                }
                """;

        mockMvc.perform(post("/api/platforms")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una plataforma con ese código"));
    }

    @Test
    @DisplayName("PUT /api/platforms/{id}: actualiza plataforma y devuelve 200 OK")
    void testActualizarPlataformaExitosoDevuelve200() throws Exception {
        PlataformaResponse actualizada = new PlataformaResponse(1L, "INDEED_COM", "Indeed Global", null, null);
        when(plataformaService.actualizar(eq(1L), any(PlataformaUpdateRequest.class))).thenReturn(actualizada);

        String jsonBody = """
                {
                    "codigo": "INDEED_COM",
                    "nombre": "Indeed Global"
                }
                """;

        mockMvc.perform(put("/api/platforms/1")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("INDEED_COM"))
                .andExpect(jsonPath("$.nombre").value("Indeed Global"));

        verify(plataformaService).actualizar(eq(1L), any(PlataformaUpdateRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/platforms/{id}: elimina plataforma y devuelve 204 No Content")
    void testEliminarPlataformaExitosoDevuelve204() throws Exception {
        doNothing().when(plataformaService).eliminar(1L);

        mockMvc.perform(delete("/api/platforms/1")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNoContent());

        verify(plataformaService).eliminar(1L);
    }

    @Test
    @DisplayName("DELETE /api/platforms/{id}: devuelve 409 Conflict si tiene candidaturas vinculadas")
    void testEliminarPlataformaConCandidaturasDevuelve409() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "No se puede eliminar la plataforma porque tiene candidaturas asociadas"))
                .when(plataformaService).eliminar(1L);

        mockMvc.perform(delete("/api/platforms/1")
                        .header("Authorization", TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede eliminar la plataforma porque tiene candidaturas asociadas"));

        verify(plataformaService).eliminar(1L);
    }
}

