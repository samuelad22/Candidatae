package com.candidatae.service;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.ProveedorCorreo;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.TermsDto.TermsAcceptanceRequest;
import com.candidatae.dto.TermsDto.TermsStatusResponse;
import com.candidatae.exception.ApiException;
import com.candidatae.integration.gmail.GmailOAuthService;
import com.candidatae.repository.CuentaCorreoRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.TokenCipherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Unit Tests")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaCorreoRepository cuentaCorreoRepository;

    @Mock
    private TokenCipherService tokenCipherService;

    @Mock
    private GmailOAuthService gmailOAuthService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .email("ana@candidatae.com")
                .name("Ana")
                .termsAccepted(false)
                .build();
    }

    @Test
    @DisplayName("getTermsStatus: devuelve estado actual de los términos")
    void testGetTermsStatus() {
        usuario.setTermsAccepted(true);
        usuario.setTermsAcceptedAt(Instant.now());
        usuario.setTermsVersion("1.0");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        TermsStatusResponse response = usuarioService.getTermsStatus(1L);

        assertTrue(response.termsAccepted());
        assertEquals("1.0", response.termsVersion());
        assertNotNull(response.termsAcceptedAt());
    }

    @Test
    @DisplayName("getTermsStatus: lanza 404 NOT_FOUND si el usuario no existe")
    void testGetTermsStatusNotFound() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> usuarioService.getTermsStatus(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("updateTermsAcceptance: registra aceptación con timestamp y versión")
    void testUpdateTermsAcceptanceAccept() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        TermsAcceptanceRequest request = new TermsAcceptanceRequest(true, "1.0");
        TermsStatusResponse response = usuarioService.updateTermsAcceptance(1L, request);

        assertTrue(response.termsAccepted());
        assertEquals("1.0", response.termsVersion());
        assertNotNull(response.termsAcceptedAt());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("updateTermsAcceptance: registra revocación limpiando timestamp y versión")
    void testUpdateTermsAcceptanceRevoke() {
        usuario.setTermsAccepted(true);
        usuario.setTermsAcceptedAt(Instant.now());
        usuario.setTermsVersion("1.0");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        TermsAcceptanceRequest request = new TermsAcceptanceRequest(false, null);
        TermsStatusResponse response = usuarioService.updateTermsAcceptance(1L, request);

        assertFalse(response.termsAccepted());
        assertNull(response.termsAcceptedAt());
        assertNull(response.termsVersion());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("deleteAccount: derecho al olvido elimina usuario y revoca tokens de Google")
    void testDeleteAccountSuccess() {
        CuentaCorreo gmail = CuentaCorreo.builder()
                .id(10L)
                .proveedor(ProveedorCorreo.GMAIL)
                .refreshTokenCifrado("encrypted".getBytes())
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(cuentaCorreoRepository.findByUsuarioIdOrderByFechaConexionDescIdDesc(1L)).thenReturn(List.of(gmail));
        when(tokenCipherService.decrypt("encrypted".getBytes())).thenReturn("plain-token");

        usuarioService.deleteAccount(1L);

        verify(tokenCipherService).decrypt("encrypted".getBytes());
        verify(gmailOAuthService).revocarToken("plain-token");
        verify(usuarioRepository).delete(usuario);
    }
}
