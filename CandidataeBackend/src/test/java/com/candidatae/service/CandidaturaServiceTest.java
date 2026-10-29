package com.candidatae.service;

import com.candidatae.domain.Candidatura;
import com.candidatae.domain.Empresa;
import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.Contacto;
import com.candidatae.domain.HistorialEstado;
import com.candidatae.domain.Modalidad;
import com.candidatae.domain.Nota;
import com.candidatae.domain.OrigenEvento;
import com.candidatae.domain.Plataforma;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CandidaturaCreateRequest;
import com.candidatae.dto.CandidaturaDetailResponse;
import com.candidatae.dto.CandidaturaResponse;
import com.candidatae.dto.CandidaturaUpdateRequest;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CandidaturaRepository;
import com.candidatae.repository.EmpresaRepository;
import com.candidatae.repository.PlataformaRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.CurrentUserProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.candidatae.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidaturaService - pruebas de lógica y filtros")
class CandidaturaServiceTest {

    @Mock
    private CandidaturaRepository candidaturaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private PlataformaRepository plataformaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CandidaturaService candidaturaService;

    @Test
    @DisplayName("crear: alta exitosa con normalización de empresa, estado inicial ENVIADA, primer historial y deduplicación Glassdoor -> Indeed")
    void testCrearCandidaturaExitosaConNormalizacionYDeduplicacionGlassdoor() {
        Long usuarioId = 10L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // Empresa con sufijo societario y mayúsculas que debe normalizarse
        String empresaEntrada = "Google España S.L.";
        String empresaNormalizada = "google españa";
        Empresa empresaGuardada = Empresa.builder()
                .id(1L)
                .nombre("Google España S.L.")
                .nombreNormalizado(empresaNormalizada)
                .build();
        when(empresaRepository.findByNombreNormalizado(empresaNormalizada)).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresaGuardada);

        // Plataforma Glassdoor que tiene como plataformaOrigen Indeed
        Plataforma plataformaIndeed = Plataforma.builder()
                .id(20L)
                .codigo("INDEED")
                .nombre("Indeed")
                .build();
        Plataforma plataformaGlassdoor = Plataforma.builder()
                .id(21L)
                .codigo("GLASSDOOR")
                .nombre("Glassdoor")
                .plataformaOrigen(plataformaIndeed)
                .build();
        when(plataformaRepository.findByCodigoIgnoreCase("GLASSDOOR")).thenReturn(Optional.of(plataformaGlassdoor));

        when(candidaturaRepository.existsByUsuarioIdAndClaveDeduplicacion(eq(usuarioId), any(String.class)))
                .thenReturn(false);

        when(candidaturaRepository.save(any(Candidatura.class))).thenAnswer(invocation -> {
            Candidatura c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        LocalDate fechaSolicitud = LocalDate.of(2026, 10, 25);
        CandidaturaCreateRequest request = new CandidaturaCreateRequest(
                empresaEntrada,
                "Backend Developer",
                "GLASSDOOR",
                "Madrid",
                Modalidad.REMOTO,
                "45.000€",
                "https://glassdoor.com/job/123",
                fechaSolicitud
        );

        CandidaturaResponse response = candidaturaService.crear(request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Backend Developer", response.puesto());
        assertEquals(EstadoCandidatura.ENVIADA, response.estadoActual());
        assertEquals("GLASSDOOR", response.plataformaCodigo());

        // Verificamos los datos persistidos en Candidatura y su HistorialEstado
        ArgumentCaptor<Candidatura> candidaturaCaptor = ArgumentCaptor.forClass(Candidatura.class);
        verify(candidaturaRepository).save(candidaturaCaptor.capture());

        Candidatura guardada = candidaturaCaptor.getValue();
        assertEquals("Google España S.L.", guardada.getEmpresa().getNombre());
        assertEquals(EstadoCandidatura.ENVIADA, guardada.getEstadoActual());
        // Deduplicación Glassdoor -> Indeed toma "indeed"
        assertEquals("google-espana:backend-developer:madrid:indeed", guardada.getClaveDeduplicacion());

        // Verifica que se creó el primer registro en HistorialEstado
        assertEquals(1, guardada.getHistoriales().size());
        HistorialEstado primerHistorial = guardada.getHistoriales().get(0);
        assertEquals(EstadoCandidatura.ENVIADA, primerHistorial.getEstadoNuevo());
        assertEquals(null, primerHistorial.getEstadoAnterior());
        assertEquals(OrigenEvento.MANUAL, primerHistorial.getOrigen());
        assertNotNull(primerHistorial.getFechaEvento());
    }

    @Test
    @DisplayName("crear: lanza ApiException 409 Conflict si ya existe una candidatura para la misma empresa, puesto y plataforma")
    void testCrearCandidaturaDuplicadaLanzaConflict409() {
        Long usuarioId = 10L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        Empresa empresa = Empresa.builder().id(1L).nombre("Google").nombreNormalizado("google").build();
        when(empresaRepository.findByNombreNormalizado("google")).thenReturn(Optional.of(empresa));

        Plataforma plataforma = Plataforma.builder().id(2L).codigo("LINKEDIN").nombre("LinkedIn").build();
        when(plataformaRepository.findByCodigoIgnoreCase("LINKEDIN")).thenReturn(Optional.of(plataforma));

        when(candidaturaRepository.existsByUsuarioIdAndClaveDeduplicacion(eq(usuarioId), any(String.class)))
                .thenReturn(true);

        CandidaturaCreateRequest request = new CandidaturaCreateRequest(
                "Google",
                "Backend Developer",
                "LINKEDIN",
                "Madrid",
                Modalidad.REMOTO,
                null,
                null,
                LocalDate.now()
        );

        ApiException ex = assertThrows(ApiException.class, () -> candidaturaService.crear(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Ya tienes registrada una candidatura para este puesto, empresa y plataforma", ex.getMessage());

        verify(candidaturaRepository, never()).save(any(Candidatura.class));
    }

    @Test
    @DisplayName("crear: lanza ApiException 400 Bad Request si el código de plataforma no existe")
    void testCrearCandidaturaPlataformaInvalidaLanzaBadRequest() {
        Long usuarioId = 10L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        Empresa empresa = Empresa.builder().id(1L).nombre("Google").nombreNormalizado("google").build();
        when(empresaRepository.findByNombreNormalizado("google")).thenReturn(Optional.of(empresa));

        when(plataformaRepository.findByCodigoIgnoreCase("INVENTADA")).thenReturn(Optional.empty());

        CandidaturaCreateRequest request = new CandidaturaCreateRequest(
                "Google",
                "Backend Developer",
                "INVENTADA",
                "Madrid",
                Modalidad.REMOTO,
                null,
                null,
                LocalDate.now()
        );

        ApiException ex = assertThrows(ApiException.class, () -> candidaturaService.crear(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("INVENTADA"));
    }

    @Test
    @DisplayName("listar: consulta con Specification y Pageable ordenado por fechaUltimoCambio DESC y filtra por usuario autenticado")
    void testListarCandidaturas() {
        Long usuarioId = 10L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        Empresa empresa = Empresa.builder().id(1L).nombre("Google").nombreNormalizado("google").build();
        Plataforma plataforma = Plataforma.builder().id(2L).codigo("LINKEDIN").nombre("LinkedIn").build();

        Candidatura candidatura = Candidatura.builder()
                .id(100L)
                .usuario(usuario)
                .empresa(empresa)
                .plataforma(plataforma)
                .puesto("Backend Developer")
                .ubicacion("Madrid")
                .modalidad(Modalidad.REMOTO)
                .fechaSolicitud(LocalDate.now())
                .estadoActual(EstadoCandidatura.ENVIADA)
                .fechaUltimoCambio(Instant.now())
                .build();

        Page<Candidatura> pageMock = new PageImpl<>(List.of(candidatura), PageRequest.of(0, 20), 1);

        when(plataformaRepository.findByCodigoIgnoreCase("LINKEDIN")).thenReturn(Optional.of(plataforma));
        when(candidaturaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pageMock);

        PageResponse<CandidaturaResponse> resultado = candidaturaService.listar(
                "LINKEDIN",
                "ENVIADA",
                "Google",
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                "Backend",
                0,
                20
        );

        assertNotNull(resultado);
        assertEquals(1, resultado.content().size());
        assertEquals(0, resultado.page());
        assertEquals(20, resultado.size());
        assertEquals(1, resultado.totalElements());
        assertEquals(1, resultado.totalPages());
        assertTrue(resultado.last());
        assertEquals(100L, resultado.content().get(0).id());
        assertEquals("Google", resultado.content().get(0).empresaNombre());
        assertEquals("LINKEDIN", resultado.content().get(0).plataformaCodigo());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(candidaturaRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        Sort.Order order = pageable.getSort().getOrderFor("fechaUltimoCambio");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    @DisplayName("listar: lanza ApiException 400 Bad Request si la paginación no es válida")
    void testListarPaginacionInvalidaLanzaBadRequest400() {
        Long usuarioId = 10L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        // Página negativa
        ApiException ex1 = assertThrows(ApiException.class, () -> candidaturaService.listar(
                null, null, null, null, null, null, -1, 20
        ));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());
        assertEquals("El número de página no puede ser negativo", ex1.getMessage());

        // Tamaño menor que 1
        ApiException ex2 = assertThrows(ApiException.class, () -> candidaturaService.listar(
                null, null, null, null, null, null, 0, 0
        ));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());
        assertEquals("El tamaño de página debe estar entre 1 y 100", ex2.getMessage());

        // Tamaño mayor que 100
        ApiException ex3 = assertThrows(ApiException.class, () -> candidaturaService.listar(
                null, null, null, null, null, null, 0, 101
        ));
        assertEquals(HttpStatus.BAD_REQUEST, ex3.getStatus());
        assertEquals("El tamaño de página debe estar entre 1 y 100", ex3.getMessage());
    }

    @Test
    @DisplayName("listar: lanza ApiException 400 Bad Request si el status no es válido")
    void testListarStatusInvalidoLanzaBadRequest400() {
        Long usuarioId = 10L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        ApiException ex = assertThrows(ApiException.class, () -> candidaturaService.listar(
                null,
                "ENVIAD",
                null,
                null,
                null,
                null,
                0,
                20
        ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Valor 'ENVIAD' no válido para 'status'"));
        assertTrue(ex.getMessage().contains("Valores permitidos: [ENVIADA, EN_PROCESO, ENTREVISTA, OFERTA, RECHAZADA, DESCARTADA]"));
    }

    @Test
    @DisplayName("listar: lanza ApiException 400 Bad Request si la plataforma no existe")
    void testListarPlataformaInvalidaLanzaBadRequest400() {
        Long usuarioId = 10L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);
        when(plataformaRepository.findByCodigoIgnoreCase("INVENTADA")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> candidaturaService.listar(
                "INVENTADA",
                null,
                null,
                null,
                null,
                null,
                0,
                20
        ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("La plataforma 'INVENTADA' no es válida"));
    }

    @Test
    @DisplayName("obtenerDetalle: devuelve el detalle completo con historial asc, notas desc y contactos si pertenece al usuario")
    void testObtenerDetalleExitoso() {
        Long usuarioId = 10L;
        Long candidaturaId = 100L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        Empresa empresa = Empresa.builder().id(1L).nombre("Google").nombreNormalizado("google").build();
        Plataforma plataforma = Plataforma.builder().id(2L).codigo("LINKEDIN").nombre("LinkedIn").build();

        Instant t1 = Instant.parse("2026-10-20T10:00:00Z");
        Instant t2 = Instant.parse("2026-10-22T10:00:00Z");

        HistorialEstado h1 = HistorialEstado.builder()
                .id(1L)
                .estadoAnterior(null)
                .estadoNuevo(EstadoCandidatura.ENVIADA)
                .fechaEvento(t1)
                .origen(OrigenEvento.MANUAL)
                .build();

        HistorialEstado h2 = HistorialEstado.builder()
                .id(2L)
                .estadoAnterior(EstadoCandidatura.ENVIADA)
                .estadoNuevo(EstadoCandidatura.EN_PROCESO)
                .fechaEvento(t2)
                .origen(OrigenEvento.AUTOMATICO)
                .build();

        Nota n1 = Nota.builder().id(10L).texto("Nota antigua").fecha(t1).build();
        Nota n2 = Nota.builder().id(11L).texto("Nota reciente").fecha(t2).build();

        Contacto c1 = Contacto.builder().id(20L).nombre("Recruiter Ana").email("ana@google.com").rol("HR").build();

        // Introducimos desorden en las listas originales para verificar que el DTO las ordena como exige la especificación
        List<HistorialEstado> historialesDesordenados = new ArrayList<>(List.of(h2, h1));
        List<Nota> notasDesordenadas = new ArrayList<>(List.of(n1, n2));
        List<Contacto> listaContactos = new ArrayList<>(List.of(c1));

        Candidatura candidatura = Candidatura.builder()
                .id(candidaturaId)
                .usuario(usuario)
                .empresa(empresa)
                .plataforma(plataforma)
                .puesto("Backend Developer")
                .ubicacion("Madrid")
                .modalidad(Modalidad.REMOTO)
                .fechaSolicitud(LocalDate.now())
                .estadoActual(EstadoCandidatura.EN_PROCESO)
                .fechaUltimoCambio(t2)
                .historiales(historialesDesordenados)
                .notas(notasDesordenadas)
                .contactos(listaContactos)
                .build();

        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId))
                .thenReturn(Optional.of(candidatura));

        CandidaturaDetailResponse detalle = candidaturaService.obtenerDetalle(candidaturaId);

        assertNotNull(detalle);
        assertEquals(candidaturaId, detalle.id());
        assertEquals("Google", detalle.empresaNombre());
        assertEquals("LINKEDIN", detalle.plataformaCodigo());

        // Historial debe estar ordenado cronológicamente ascendente (t1 antes que t2)
        assertEquals(2, detalle.historial().size());
        assertEquals(1L, detalle.historial().get(0).id());
        assertEquals(2L, detalle.historial().get(1).id());
        assertTrue(detalle.historial().get(0).fechaEvento().isBefore(detalle.historial().get(1).fechaEvento()));

        // Notas deben estar ordenadas por fecha descendente (t2 antes que t1)
        assertEquals(2, detalle.notas().size());
        assertEquals(11L, detalle.notas().get(0).id());
        assertEquals(10L, detalle.notas().get(1).id());
        assertTrue(detalle.notas().get(0).fecha().isAfter(detalle.notas().get(1).fecha()));

        // Contactos
        assertEquals(1, detalle.contactos().size());
        assertEquals("Recruiter Ana", detalle.contactos().get(0).nombre());
    }

    @Test
    @DisplayName("obtenerDetalle: lanza ApiException 404 si la candidatura no existe o no pertenece al usuario")
    void testObtenerDetalleNoEncontrado() {
        Long usuarioId = 10L;
        Long candidaturaId = 999L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);
        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> candidaturaService.obtenerDetalle(candidaturaId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Candidatura no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("actualizar: al cambiar de estado añade automáticamente una nueva fila en el historial")
    void testActualizarCambioEstadoRegistraHistorial() {
        Long usuarioId = 10L;
        Long candidaturaId = 100L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        Empresa empresa = Empresa.builder().id(1L).nombre("Google").nombreNormalizado("google").build();

        Instant t0 = Instant.parse("2026-10-20T10:00:00Z");
        HistorialEstado hInicial = HistorialEstado.builder()
                .id(1L)
                .estadoAnterior(null)
                .estadoNuevo(EstadoCandidatura.ENVIADA)
                .fechaEvento(t0)
                .origen(OrigenEvento.MANUAL)
                .build();

        Candidatura candidatura = Candidatura.builder()
                .id(candidaturaId)
                .usuario(usuario)
                .empresa(empresa)
                .puesto("Backend Developer")
                .ubicacion("Madrid")
                .modalidad(Modalidad.REMOTO)
                .fechaSolicitud(LocalDate.now())
                .estadoActual(EstadoCandidatura.ENVIADA)
                .fechaUltimoCambio(t0)
                .historiales(new ArrayList<>(List.of(hInicial)))
                .build();

        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId))
                .thenReturn(Optional.of(candidatura));
        when(candidaturaRepository.save(any(Candidatura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CandidaturaUpdateRequest updateRequest = new CandidaturaUpdateRequest(
                null, null, null, null, null, null, null, null, EstadoCandidatura.ENTREVISTA
        );

        CandidaturaResponse response = candidaturaService.actualizar(candidaturaId, updateRequest);

        assertNotNull(response);
        assertEquals(EstadoCandidatura.ENTREVISTA, response.estadoActual());
        assertEquals(EstadoCandidatura.ENTREVISTA, candidatura.getEstadoActual());

        // Debe haberse añadido el segundo evento en el historial
        assertEquals(2, candidatura.getHistoriales().size());
        HistorialEstado nuevoHistorial = candidatura.getHistoriales().get(1);
        assertEquals(EstadoCandidatura.ENVIADA, nuevoHistorial.getEstadoAnterior());
        assertEquals(EstadoCandidatura.ENTREVISTA, nuevoHistorial.getEstadoNuevo());
        assertEquals(OrigenEvento.MANUAL, nuevoHistorial.getOrigen());
        assertNotNull(nuevoHistorial.getFechaEvento());
        assertTrue(candidatura.getFechaUltimoCambio().isAfter(t0));
    }

    @Test
    @DisplayName("actualizar: al editar un campo sin cambiar el estado NO se duplica el historial")
    void testActualizarSinCambioEstadoNoModificaHistorial() {
        Long usuarioId = 10L;
        Long candidaturaId = 100L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        Empresa empresa = Empresa.builder().id(1L).nombre("Google").nombreNormalizado("google").build();

        Instant t0 = Instant.parse("2026-10-20T10:00:00Z");
        HistorialEstado hInicial = HistorialEstado.builder()
                .id(1L)
                .estadoAnterior(null)
                .estadoNuevo(EstadoCandidatura.ENVIADA)
                .fechaEvento(t0)
                .origen(OrigenEvento.MANUAL)
                .build();

        Candidatura candidatura = Candidatura.builder()
                .id(candidaturaId)
                .usuario(usuario)
                .empresa(empresa)
                .puesto("Backend Developer")
                .ubicacion("Madrid")
                .modalidad(Modalidad.REMOTO)
                .salarioTexto("35.000€")
                .fechaSolicitud(LocalDate.now())
                .estadoActual(EstadoCandidatura.ENVIADA)
                .fechaUltimoCambio(t0)
                .historiales(new ArrayList<>(List.of(hInicial)))
                .build();

        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId))
                .thenReturn(Optional.of(candidatura));
        when(candidaturaRepository.save(any(Candidatura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Actualizamos solo salarioTexto y mantenemos el mismo estadoActual
        CandidaturaUpdateRequest updateRequest = new CandidaturaUpdateRequest(
                null, null, null, null, null, "50.000€", null, null, EstadoCandidatura.ENVIADA
        );

        CandidaturaResponse response = candidaturaService.actualizar(candidaturaId, updateRequest);

        assertNotNull(response);
        assertEquals("50.000€", response.salarioTexto());
        assertEquals("50.000€", candidatura.getSalarioTexto());
        assertEquals(EstadoCandidatura.ENVIADA, response.estadoActual());

        // El historial NO debe tener filas nuevas
        assertEquals(1, candidatura.getHistoriales().size());
    }

    @Test
    @DisplayName("actualizar: lanza ApiException 404 si la candidatura no existe o no pertenece al usuario")
    void testActualizarNoEncontradoLanza404() {
        Long usuarioId = 10L;
        Long candidaturaId = 999L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);
        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId)).thenReturn(Optional.empty());

        CandidaturaUpdateRequest request = new CandidaturaUpdateRequest(
                null, null, null, null, null, null, null, null, EstadoCandidatura.OFERTA
        );

        ApiException ex = assertThrows(ApiException.class, () -> candidaturaService.actualizar(candidaturaId, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Candidatura no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("actualizar: al cambiar empresa o puesto se recalcula la clave de deduplicación")
    void testActualizarEmpresaYPuestoRecalculaClaveDeduplicacion() {
        Long usuarioId = 10L;
        Long candidaturaId = 100L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        Empresa empresaVieja = Empresa.builder().id(1L).nombre("Startup").nombreNormalizado("startup").build();
        Empresa empresaNueva = Empresa.builder().id(2L).nombre("BigTech S.L.").nombreNormalizado("bigtech").build();

        Candidatura candidatura = Candidatura.builder()
                .id(candidaturaId)
                .usuario(usuario)
                .empresa(empresaVieja)
                .puesto("Junior Dev")
                .ubicacion("Madrid")
                .fechaSolicitud(LocalDate.now())
                .estadoActual(EstadoCandidatura.ENVIADA)
                .claveDeduplicacion("startup:junior-dev:madrid")
                .fechaUltimoCambio(Instant.now())
                .build();

        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId))
                .thenReturn(Optional.of(candidatura));
        when(empresaRepository.findByNombreNormalizado("bigtech"))
                .thenReturn(Optional.of(empresaNueva));
        when(candidaturaRepository.save(any(Candidatura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CandidaturaUpdateRequest request = new CandidaturaUpdateRequest(
                "BigTech S.L.", null, "Senior Tech Lead", "Barcelona", null, null, null, null, null
        );

        CandidaturaResponse response = candidaturaService.actualizar(candidaturaId, request);

        assertNotNull(response);
        assertEquals("BigTech S.L.", response.empresaNombre());
        assertEquals("Senior Tech Lead", response.puesto());
        assertEquals("Barcelona", response.ubicacion());
        assertEquals("bigtech:senior-tech-lead:barcelona", candidatura.getClaveDeduplicacion());
    }

    @Test
    @DisplayName("actualizar: lanza ApiException 409 Conflict si al recalcular clave ya existe otra candidatura del usuario")
    void testActualizarEmpresaYPuestoDuplicadoLanzaConflict409() {
        Long usuarioId = 10L;
        Long candidaturaId = 100L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        Empresa empresaVieja = Empresa.builder().id(1L).nombre("Startup").nombreNormalizado("startup").build();
        Empresa empresaNueva = Empresa.builder().id(2L).nombre("BigTech S.L.").nombreNormalizado("bigtech").build();

        Candidatura candidatura = Candidatura.builder()
                .id(candidaturaId)
                .usuario(usuario)
                .empresa(empresaVieja)
                .puesto("Junior Dev")
                .ubicacion("Madrid")
                .fechaSolicitud(LocalDate.now())
                .estadoActual(EstadoCandidatura.ENVIADA)
                .claveDeduplicacion("startup:junior-dev:madrid")
                .fechaUltimoCambio(Instant.now())
                .build();

        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId))
                .thenReturn(Optional.of(candidatura));
        when(empresaRepository.findByNombreNormalizado("bigtech"))
                .thenReturn(Optional.of(empresaNueva));
        when(candidaturaRepository.existsByUsuarioIdAndClaveDeduplicacion(eq(usuarioId), eq("bigtech:senior-tech-lead:barcelona")))
                .thenReturn(true);

        CandidaturaUpdateRequest request = new CandidaturaUpdateRequest(
                "BigTech S.L.", null, "Senior Tech Lead", "Barcelona", null, null, null, null, null
        );

        ApiException ex = assertThrows(ApiException.class, () -> candidaturaService.actualizar(candidaturaId, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Ya tienes registrada otra candidatura para este puesto, empresa y plataforma", ex.getMessage());

        verify(candidaturaRepository, never()).save(any(Candidatura.class));
    }

    @Test
    @DisplayName("actualizar: vacía campos opcionales si vienen como cadena vacía o en blanco")
    void testActualizarVaciarCamposOpcionalesConCadenaVacia() {
        Long usuarioId = 10L;
        Long candidaturaId = 100L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        Empresa empresa = Empresa.builder().id(1L).nombre("Google").nombreNormalizado("google").build();
        Plataforma plataforma = Plataforma.builder().id(2L).codigo("LINKEDIN").nombre("LinkedIn").build();

        Candidatura candidatura = Candidatura.builder()
                .id(candidaturaId)
                .usuario(usuario)
                .empresa(empresa)
                .plataforma(plataforma)
                .puesto("Backend Developer")
                .ubicacion("Madrid")
                .salarioTexto("40.000€")
                .enlaceOferta("https://linkedin.com/jobs/123")
                .modalidad(Modalidad.HIBRIDO)
                .fechaSolicitud(LocalDate.now())
                .estadoActual(EstadoCandidatura.ENVIADA)
                .claveDeduplicacion("google:backend-developer:madrid:linkedin")
                .fechaUltimoCambio(Instant.now())
                .build();

        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId))
                .thenReturn(Optional.of(candidatura));
        when(candidaturaRepository.save(any(Candidatura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Enviamos cadenas vacías o en blanco en los campos opcionales
        CandidaturaUpdateRequest request = new CandidaturaUpdateRequest(
                null,
                "   ",  // vaciar plataforma
                null,
                "",     // vaciar ubicacion
                null,
                " ",    // vaciar salarioTexto
                "",     // vaciar enlaceOferta
                null,
                null
        );

        CandidaturaResponse response = candidaturaService.actualizar(candidaturaId, request);

        assertNotNull(response);
        assertNull(response.plataformaCodigo());
        assertNull(response.ubicacion());
        assertNull(response.salarioTexto());
        assertNull(response.enlaceOferta());
        assertNull(candidatura.getPlataforma());
        assertNull(candidatura.getUbicacion());
        assertNull(candidatura.getSalarioTexto());
        assertNull(candidatura.getEnlaceOferta());
        // Se recalculó la deduplicación al cambiar plataforma y ubicación
        assertEquals("google:backend-developer:sin-ubicacion", candidatura.getClaveDeduplicacion());
    }

    @Test
    @DisplayName("eliminar: borra la candidatura de forma exitosa si pertenece al usuario")
    void testEliminarExitoso() {
        Long usuarioId = 10L;
        Long candidaturaId = 100L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);

        Usuario usuario = Usuario.builder().id(usuarioId).email("test@example.com").build();
        Candidatura candidatura = Candidatura.builder()
                .id(candidaturaId)
                .usuario(usuario)
                .puesto("Backend Developer")
                .fechaSolicitud(LocalDate.now())
                .estadoActual(EstadoCandidatura.ENVIADA)
                .build();

        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId))
                .thenReturn(Optional.of(candidatura));

        candidaturaService.eliminar(candidaturaId);

        verify(candidaturaRepository).delete(candidatura);
    }

    @Test
    @DisplayName("eliminar: lanza ApiException 404 si la candidatura no existe o no pertenece al usuario")
    void testEliminarNoEncontradoLanza404() {
        Long usuarioId = 10L;
        Long candidaturaId = 999L;
        when(currentUserProvider.requireUserId()).thenReturn(usuarioId);
        when(candidaturaRepository.findByIdAndUsuarioId(candidaturaId, usuarioId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> candidaturaService.eliminar(candidaturaId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Candidatura no encontrada", ex.getMessage());

        verify(candidaturaRepository, never()).delete(any(Candidatura.class));
    }
}
