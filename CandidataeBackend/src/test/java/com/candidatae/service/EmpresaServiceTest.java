package com.candidatae.service;

import com.candidatae.domain.Empresa;
import com.candidatae.dto.EmpresaDto.EmpresaCreateRequest;
import com.candidatae.dto.EmpresaDto.EmpresaResponse;
import com.candidatae.dto.EmpresaDto.EmpresaUpdateRequest;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CandidaturaRepository;
import com.candidatae.repository.EmpresaRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaService - Unit Tests")
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private CandidaturaRepository candidaturaRepository;

    @InjectMocks
    private EmpresaService empresaService;

    private Empresa empresaGoogle;
    private Empresa empresaAmazon;

    @BeforeEach
    void setUp() {
        empresaGoogle = Empresa.builder()
                .id(1L)
                .nombre("Google Spain S.L.")
                .nombreNormalizado("google spain")
                .build();

        empresaAmazon = Empresa.builder()
                .id(2L)
                .nombre("Amazon Web Services")
                .nombreNormalizado("amazon web services")
                .build();
    }

    @Test
    @DisplayName("listar: devuelve todas las empresas ordenadas alfabéticamente si no hay término de búsqueda")
    void testListarSinBusqueda() {
        when(empresaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(empresaAmazon, empresaGoogle));

        List<EmpresaResponse> resultado = empresaService.listar(null);

        assertEquals(2, resultado.size());
        assertEquals("Amazon Web Services", resultado.get(0).nombre());
        assertEquals("Google Spain S.L.", resultado.get(1).nombre());
        verify(empresaRepository).findAllByOrderByNombreAsc();
    }

    @Test
    @DisplayName("listar: filtra empresas por coincidencia parcial si se indica término de búsqueda")
    void testListarConBusqueda() {
        when(empresaRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc("goog"))
                .thenReturn(List.of(empresaGoogle));

        List<EmpresaResponse> resultado = empresaService.listar("goog");

        assertEquals(1, resultado.size());
        assertEquals("Google Spain S.L.", resultado.get(0).nombre());
        verify(empresaRepository).findByNombreContainingIgnoreCaseOrderByNombreAsc("goog");
    }

    @Test
    @DisplayName("obtenerPorId: devuelve la empresa si existe")
    void testObtenerPorIdExitoso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaGoogle));

        EmpresaResponse response = empresaService.obtenerPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Google Spain S.L.", response.nombre());
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 NOT_FOUND si la empresa no existe")
    void testObtenerPorIdNoExiste() {
        when(empresaRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> empresaService.obtenerPorId(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("crear: registra correctamente una nueva empresa con nombre normalizado")
    void testCrearExitoso() {
        EmpresaCreateRequest request = new EmpresaCreateRequest("Spotify S.A.");
        when(empresaRepository.existsByNombreNormalizado(anyString())).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa e = invocation.getArgument(0);
            e.setId(3L);
            return e;
        });

        EmpresaResponse response = empresaService.crear(request);

        assertNotNull(response);
        assertEquals(3L, response.id());
        assertEquals("Spotify S.A.", response.nombre());
        assertEquals("spotify", response.nombreNormalizado());
        verify(empresaRepository).save(any(Empresa.class));
    }

    @Test
    @DisplayName("crear: lanza 409 CONFLICT si ya existe una empresa con el mismo nombre normalizado")
    void testCrearConflicto() {
        EmpresaCreateRequest request = new EmpresaCreateRequest("Google Spain S.L.");
        when(empresaRepository.existsByNombreNormalizado("google spain")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> empresaService.crear(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    @DisplayName("actualizar: modifica el nombre de una empresa existente y renormailza")
    void testActualizarExitoso() {
        EmpresaUpdateRequest request = new EmpresaUpdateRequest("Google LLC");
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaGoogle));
        when(empresaRepository.existsByNombreNormalizadoAndIdNot("google", 1L)).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));

        EmpresaResponse response = empresaService.actualizar(1L, request);

        assertEquals("Google LLC", response.nombre());
        assertEquals("google", response.nombreNormalizado());
        verify(empresaRepository).save(empresaGoogle);
    }

    @Test
    @DisplayName("actualizar: lanza 409 CONFLICT si el nuevo nombre colisiona con otra empresa")
    void testActualizarConflicto() {
        EmpresaUpdateRequest request = new EmpresaUpdateRequest("Amazon Web Services S.L.");
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaGoogle));
        when(empresaRepository.existsByNombreNormalizadoAndIdNot("amazon web services", 1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> empresaService.actualizar(1L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(empresaRepository, never()).save(any(Empresa.class));
    }

    @Test
    @DisplayName("eliminar: borra la empresa si existe y no tiene candidaturas asociadas")
    void testEliminarExitoso() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaGoogle));
        when(candidaturaRepository.existsByEmpresaId(1L)).thenReturn(false);

        empresaService.eliminar(1L);

        verify(empresaRepository).delete(empresaGoogle);
    }

    @Test
    @DisplayName("eliminar: lanza 409 CONFLICT si la empresa tiene candidaturas asociadas")
    void testEliminarConflictoCandidaturas() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaGoogle));
        when(candidaturaRepository.existsByEmpresaId(1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> empresaService.eliminar(1L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(empresaRepository, never()).delete(any(Empresa.class));
    }

    @Test
    @DisplayName("eliminar: lanza 404 NOT_FOUND si la empresa no existe")
    void testEliminarNoExiste() {
        when(empresaRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> empresaService.eliminar(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(empresaRepository, never()).delete(any(Empresa.class));
    }

    @Test
    @DisplayName("normalizarNombre: elimina sufijos legales y colapsa espacios")
    void testNormalizarNombre() {
        assertEquals("inditex", empresaService.normalizarNombre("Inditex S.A."));
        assertEquals("mercadona", empresaService.normalizarNombre("  Mercadona, S.L.U.  "));
        assertEquals("apple", empresaService.normalizarNombre("Apple Inc."));
        assertEquals("microsoft", empresaService.normalizarNombre("Microsoft Corp."));
    }
}

