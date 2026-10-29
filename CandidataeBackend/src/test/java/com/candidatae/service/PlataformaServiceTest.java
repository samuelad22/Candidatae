package com.candidatae.service;

import com.candidatae.domain.Plataforma;
import com.candidatae.dto.PlataformaDto.PlataformaCreateRequest;
import com.candidatae.dto.PlataformaDto.PlataformaResponse;
import com.candidatae.dto.PlataformaDto.PlataformaUpdateRequest;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CandidaturaRepository;
import com.candidatae.repository.PlataformaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlataformaService - Unit Tests")
class PlataformaServiceTest {

    @Mock
    private PlataformaRepository plataformaRepository;

    @Mock
    private CandidaturaRepository candidaturaRepository;

    @InjectMocks
    private PlataformaService plataformaService;

    private Plataforma indeed;
    private Plataforma glassdoor;
    private Plataforma linkedin;

    @BeforeEach
    void setUp() {
        indeed = Plataforma.builder()
                .id(1L)
                .codigo("INDEED")
                .nombre("Indeed")
                .build();

        glassdoor = Plataforma.builder()
                .id(2L)
                .codigo("GLASSDOOR")
                .nombre("Glassdoor")
                .plataformaOrigen(indeed)
                .build();

        linkedin = Plataforma.builder()
                .id(3L)
                .codigo("LINKEDIN")
                .nombre("LinkedIn")
                .build();
    }

    @Test
    @DisplayName("listar: devuelve todas las plataformas ordenadas alfabeticamente si no hay busqueda")
    void testListarSinBusqueda() {
        when(plataformaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(glassdoor, indeed, linkedin));

        List<PlataformaResponse> resultado = plataformaService.listar(null);

        assertEquals(3, resultado.size());
        assertEquals("Glassdoor", resultado.get(0).nombre());
        assertEquals("Indeed", resultado.get(0).plataformaOrigenNombre());
        verify(plataformaRepository).findAllByOrderByNombreAsc();
    }

    @Test
    @DisplayName("listar: filtra plataformas por coincidencia si se indica search")
    void testListarConBusqueda() {
        when(plataformaRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc("link"))
                .thenReturn(List.of(linkedin));

        List<PlataformaResponse> resultado = plataformaService.listar("link");

        assertEquals(1, resultado.size());
        assertEquals("LinkedIn", resultado.get(0).nombre());
        verify(plataformaRepository).findByNombreContainingIgnoreCaseOrderByNombreAsc("link");
    }

    @Test
    @DisplayName("obtenerPorId: devuelve la plataforma si existe")
    void testObtenerPorIdExitoso() {
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(indeed));

        PlataformaResponse response = plataformaService.obtenerPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("INDEED", response.codigo());
        assertEquals("Indeed", response.nombre());
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 NOT_FOUND si la plataforma no existe")
    void testObtenerPorIdNoExiste() {
        when(plataformaRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> plataformaService.obtenerPorId(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("obtenerPorCodigo: devuelve la plataforma si existe")
    void testObtenerPorCodigoExitoso() {
        when(plataformaRepository.findByCodigoIgnoreCase("LINKEDIN")).thenReturn(Optional.of(linkedin));

        PlataformaResponse response = plataformaService.obtenerPorCodigo("LINKEDIN");

        assertNotNull(response);
        assertEquals("LINKEDIN", response.codigo());
        assertEquals("LinkedIn", response.nombre());
    }

    @Test
    @DisplayName("obtenerPorCodigo: lanza 404 NOT_FOUND si la plataforma no existe")
    void testObtenerPorCodigoNoExiste() {
        when(plataformaRepository.findByCodigoIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> plataformaService.obtenerPorCodigo("UNKNOWN"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("crear: registra una nueva plataforma con codigo en mayusculas")
    void testCrearExitoso() {
        PlataformaCreateRequest request = new PlataformaCreateRequest("infojobs", "InfoJobs", null);
        when(plataformaRepository.existsByCodigoIgnoreCase("INFOJOBS")).thenReturn(false);
        when(plataformaRepository.save(any(Plataforma.class))).thenAnswer(inv -> {
            Plataforma p = inv.getArgument(0);
            p.setId(4L);
            return p;
        });

        PlataformaResponse response = plataformaService.crear(request);

        assertNotNull(response);
        assertEquals(4L, response.id());
        assertEquals("INFOJOBS", response.codigo());
        assertEquals("InfoJobs", response.nombre());
        assertNull(response.plataformaOrigenId());
        verify(plataformaRepository).save(any(Plataforma.class));
    }

    @Test
    @DisplayName("crear: registra una nueva plataforma con plataforma origen vinculada")
    void testCrearConOrigen() {
        PlataformaCreateRequest request = new PlataformaCreateRequest("GLASSDOOR", "Glassdoor", 1L);
        when(plataformaRepository.existsByCodigoIgnoreCase("GLASSDOOR")).thenReturn(false);
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(indeed));
        when(plataformaRepository.save(any(Plataforma.class))).thenAnswer(inv -> {
            Plataforma p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        PlataformaResponse response = plataformaService.crear(request);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals(1L, response.plataformaOrigenId());
        assertEquals("Indeed", response.plataformaOrigenNombre());
    }

    @Test
    @DisplayName("crear: lanza 409 CONFLICT si el codigo ya existe")
    void testCrearConflicto() {
        PlataformaCreateRequest request = new PlataformaCreateRequest("LINKEDIN", "LinkedIn", null);
        when(plataformaRepository.existsByCodigoIgnoreCase("LINKEDIN")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> plataformaService.crear(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(plataformaRepository, never()).save(any(Plataforma.class));
    }

    @Test
    @DisplayName("crear: lanza 400 BAD_REQUEST si la plataforma de origen no existe")
    void testCrearOrigenInexistente() {
        PlataformaCreateRequest request = new PlataformaCreateRequest("NUEVA", "Nueva", 999L);
        when(plataformaRepository.existsByCodigoIgnoreCase("NUEVA")).thenReturn(false);
        when(plataformaRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> plataformaService.crear(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(plataformaRepository, never()).save(any(Plataforma.class));
    }

    @Test
    @DisplayName("actualizar: modifica nombre y codigo de una plataforma existente")
    void testActualizarExitoso() {
        PlataformaUpdateRequest request = new PlataformaUpdateRequest("INDEED_GLOBAL", "Indeed Jobs", null);
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(indeed));
        when(plataformaRepository.existsByCodigoIgnoreCaseAndIdNot("INDEED_GLOBAL", 1L)).thenReturn(false);
        when(plataformaRepository.save(any(Plataforma.class))).thenAnswer(inv -> inv.getArgument(0));

        PlataformaResponse response = plataformaService.actualizar(1L, request);

        assertEquals("INDEED_GLOBAL", response.codigo());
        assertEquals("Indeed Jobs", response.nombre());
        verify(plataformaRepository).save(indeed);
    }

    @Test
    @DisplayName("actualizar: lanza 400 BAD_REQUEST si intenta asignarse a si misma como origen")
    void testActualizarAutoReferenciaCircular() {
        PlataformaUpdateRequest request = new PlataformaUpdateRequest("INDEED", "Indeed", 1L);
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(indeed));

        ApiException ex = assertThrows(ApiException.class, () -> plataformaService.actualizar(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(plataformaRepository, never()).save(any(Plataforma.class));
    }

    @Test
    @DisplayName("actualizar: lanza 409 CONFLICT si el nuevo codigo ya lo utiliza otra plataforma")
    void testActualizarCodigoDuplicado() {
        PlataformaUpdateRequest request = new PlataformaUpdateRequest("LINKEDIN", "Indeed", null);
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(indeed));
        when(plataformaRepository.existsByCodigoIgnoreCaseAndIdNot("LINKEDIN", 1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> plataformaService.actualizar(1L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(plataformaRepository, never()).save(any(Plataforma.class));
    }

    @Test
    @DisplayName("eliminar: borra la plataforma si no tiene candidaturas vinculadas")
    void testEliminarExitoso() {
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(indeed));
        when(candidaturaRepository.existsByPlataformaId(1L)).thenReturn(false);
        when(plataformaRepository.findByPlataformaOrigenId(1L)).thenReturn(List.of(glassdoor));

        plataformaService.eliminar(1L);

        assertNull(glassdoor.getPlataformaOrigen());
        verify(plataformaRepository).save(glassdoor);
        verify(plataformaRepository).delete(indeed);
    }

    @Test
    @DisplayName("eliminar: lanza 409 CONFLICT si tiene candidaturas asociadas")
    void testEliminarConflictoCandidaturas() {
        when(plataformaRepository.findById(1L)).thenReturn(Optional.of(indeed));
        when(candidaturaRepository.existsByPlataformaId(1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> plataformaService.eliminar(1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(plataformaRepository, never()).delete(any(Plataforma.class));
    }

    @Test
    @DisplayName("eliminar: lanza 404 NOT_FOUND si la plataforma no existe")
    void testEliminarNoExiste() {
        when(plataformaRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> plataformaService.eliminar(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(plataformaRepository, never()).delete(any(Plataforma.class));
    }
}

