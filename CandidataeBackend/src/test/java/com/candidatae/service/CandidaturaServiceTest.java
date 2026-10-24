package com.candidatae.service;

import com.candidatae.domain.Candidatura;
import com.candidatae.domain.Empresa;
import com.candidatae.domain.EstadoCandidatura;
import com.candidatae.domain.Modalidad;
import com.candidatae.domain.Plataforma;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CandidaturaResponse;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
