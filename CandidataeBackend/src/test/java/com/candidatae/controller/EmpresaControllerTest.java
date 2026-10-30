package com.candidatae.controller;

import com.candidatae.config.SecurityConfig;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.EmpresaDto.EmpresaCreateRequest;
import com.candidatae.dto.EmpresaDto.EmpresaResponse;
import com.candidatae.dto.EmpresaDto.EmpresaUpdateRequest;
import com.candidatae.exception.ApiException;
import com.candidatae.exception.GlobalExceptionHandler;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.CurrentUserProvider;
import com.candidatae.security.JsonAccessDeniedHandler;
import com.candidatae.security.JsonAuthenticationEntryPoint;
import com.candidatae.security.JwtAuthenticationFilter;
import com.candidatae.security.JwtService;
import com.candidatae.service.EmpresaService;
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

@WebMvcTest(controllers = EmpresaController.class)
@ContextConfiguration(classes = {
        EmpresaController.class,
        CurrentUserProvider.class,
        GlobalExceptionHandler.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@DisplayName("EmpresaController - tests de la API REST (/api/companies)")
class EmpresaControllerTest {

    private static final Long USUARIO_ID = 1L;
    private static final String TOKEN = "Bearer token-valido";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmpresaService empresaService;

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
    @DisplayName("GET /api/companies: devuelve 200 OK con el listado alfabético")
    void testListarEmpresasSinBusquedaDevuelve200() throws Exception {
        List<EmpresaResponse> empresas = List.of(
                new EmpresaResponse(1L, "Amazon Web Services", "amazon web services"),
                new EmpresaResponse(2L, "Google Spain S.L.", "google spain")
        );
        when(empresaService.listar(null)).thenReturn(empresas);

        mockMvc.perform(get("/api/companies")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Amazon Web Services"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].nombre").value("Google Spain S.L."));

        verify(empresaService).listar(null);
    }

    @Test
    @DisplayName("GET /api/empresas: soporta también la ruta en español como alias")
    void testListarEmpresasRutaEspanolAliasDevuelve200() throws Exception {
        List<EmpresaResponse> empresas = List.of(
                new EmpresaResponse(1L, "Amazon Web Services", "amazon web services")
        );
        when(empresaService.listar(null)).thenReturn(empresas);

        mockMvc.perform(get("/api/empresas")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(empresaService).listar(null);
    }

    @Test
    @DisplayName("GET /api/companies?search=goog: devuelve 200 OK filtrado por término")
    void testListarEmpresasConBusquedaDevuelve200() throws Exception {
        List<EmpresaResponse> empresas = List.of(
                new EmpresaResponse(2L, "Google Spain S.L.", "google spain")
        );
        when(empresaService.listar("goog")).thenReturn(empresas);

        mockMvc.perform(get("/api/companies")
                        .param("search", "goog")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Google Spain S.L."));

        verify(empresaService).listar("goog");
    }

    @Test
    @DisplayName("GET /api/companies/{id}: devuelve 200 OK con la empresa solicitada")
    void testObtenerPorIdExitosoDevuelve200() throws Exception {
        EmpresaResponse empresa = new EmpresaResponse(1L, "Google", "google");
        when(empresaService.obtenerPorId(1L)).thenReturn(empresa);

        mockMvc.perform(get("/api/companies/1")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Google"));

        verify(empresaService).obtenerPorId(1L);
    }

    @Test
    @DisplayName("GET /api/companies/{id}: devuelve 404 NOT_FOUND si la empresa no existe")
    void testObtenerPorIdNoExisteDevuelve404() throws Exception {
        when(empresaService.obtenerPorId(999L))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Empresa no encontrada con id: 999"));

        mockMvc.perform(get("/api/companies/999")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Empresa no encontrada con id: 999"));

        verify(empresaService).obtenerPorId(999L);
    }

    @Test
    @DisplayName("POST /api/companies: registra una nueva empresa y devuelve 201 Created")
    void testCrearEmpresaExitosoDevuelve201() throws Exception {
        EmpresaResponse creada = new EmpresaResponse(10L, "Acme Corp S.A.", "acme corp");
        when(empresaService.crear(any(EmpresaCreateRequest.class))).thenReturn(creada);

        String jsonBody = """
                {
                    "nombre": "Acme Corp S.A."
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Acme Corp S.A."))
                .andExpect(jsonPath("$.nombreNormalizado").value("acme corp"));

        verify(empresaService).crear(any(EmpresaCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/companies: acepta alias en inglés como 'name' en el JSON del cuerpo")
    void testCrearEmpresaConAliasInglesName() throws Exception {
        EmpresaResponse creada = new EmpresaResponse(11L, "Netflix", "netflix");
        when(empresaService.crear(any(EmpresaCreateRequest.class))).thenReturn(creada);

        String jsonBody = """
                {
                    "name": "Netflix"
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Netflix"));

        verify(empresaService).crear(any(EmpresaCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/companies: devuelve 400 Bad Request si el nombre está en blanco")
    void testCrearEmpresaNombreVacioDevuelve400() throws Exception {
        String jsonBody = """
                {
                    "nombre": "   "
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/companies: devuelve 409 Conflict si la empresa ya está registrada")
    void testCrearEmpresaDuplicadaDevuelve409() throws Exception {
        when(empresaService.crear(any(EmpresaCreateRequest.class)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Ya existe una empresa registrada con ese nombre"));

        String jsonBody = """
                {
                    "nombre": "Google"
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una empresa registrada con ese nombre"));
    }

    @Test
    @DisplayName("PUT /api/companies/{id}: actualiza el nombre y devuelve 200 OK")
    void testActualizarEmpresaExitosoDevuelve200() throws Exception {
        EmpresaResponse actualizada = new EmpresaResponse(1L, "Google Cloud Iberia", "google cloud iberia");
        when(empresaService.actualizar(eq(1L), any(EmpresaUpdateRequest.class))).thenReturn(actualizada);

        String jsonBody = """
                {
                    "nombre": "Google Cloud Iberia"
                }
                """;

        mockMvc.perform(put("/api/companies/1")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Google Cloud Iberia"));

        verify(empresaService).actualizar(eq(1L), any(EmpresaUpdateRequest.class));
    }

    @Test
    @DisplayName("PUT /api/companies/{id}: devuelve 409 Conflict si el nuevo nombre colisiona con otra empresa")
    void testActualizarEmpresaConflictoDevuelve409() throws Exception {
        when(empresaService.actualizar(eq(1L), any(EmpresaUpdateRequest.class)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Ya existe otra empresa con ese nombre"));

        String jsonBody = """
                {
                    "nombre": "Microsoft"
                }
                """;

        mockMvc.perform(put("/api/companies/1")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe otra empresa con ese nombre"));
    }

    @Test
    @DisplayName("DELETE /api/companies/{id}: elimina empresa sin candidaturas y devuelve 204 No Content")
    void testEliminarEmpresaExitosoDevuelve204() throws Exception {
        doNothing().when(empresaService).eliminar(1L);

        mockMvc.perform(delete("/api/companies/1")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNoContent());

        verify(empresaService).eliminar(1L);
    }

    @Test
    @DisplayName("DELETE /api/companies/{id}: devuelve 409 Conflict si la empresa tiene candidaturas asociadas")
    void testEliminarEmpresaConCandidaturasDevuelve409() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "No se puede eliminar la empresa porque tiene candidaturas asociadas"))
                .when(empresaService).eliminar(1L);

        mockMvc.perform(delete("/api/companies/1")
                        .header("Authorization", TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede eliminar la empresa porque tiene candidaturas asociadas"));

        verify(empresaService).eliminar(1L);
    }
}
