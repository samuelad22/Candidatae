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
import com.candidatae.dto.CandidaturaDetailResponse;
import com.candidatae.dto.CandidaturaResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("listar: consulta con Specification ordenado por fechaUltimoCambio DESC y filtra por usuario autenticado")
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

        when(candidaturaRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(candidatura));

        List<CandidaturaResponse> resultado = candidaturaService.listar(
                "LINKEDIN",
                EstadoCandidatura.ENVIADA,
                "Google",
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                "Backend"
        );

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(100L, resultado.get(0).id());
        assertEquals("Google", resultado.get(0).empresaNombre());
        assertEquals("LINKEDIN", resultado.get(0).plataformaCodigo());

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(candidaturaRepository).findAll(any(Specification.class), sortCaptor.capture());

        Sort sort = sortCaptor.getValue();
        Sort.Order order = sort.getOrderFor("fechaUltimoCambio");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
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
}
