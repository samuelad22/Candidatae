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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Servicio de negocio para la gestión de usuarios, consentimiento legal y derecho al olvido (RGPD).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaCorreoRepository cuentaCorreoRepository;
    private final TokenCipherService tokenCipherService;
    private final GmailOAuthService gmailOAuthService;

    /**
     * Obtiene el estado del consentimiento de términos y condiciones del usuario.
     */
    @Transactional(readOnly = true)
    public TermsStatusResponse getTermsStatus(Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        return new TermsStatusResponse(
                usuario.isTermsAccepted(),
                usuario.getTermsAcceptedAt(),
                usuario.getTermsVersion()
        );
    }

    /**
     * Registra la aceptación o revocación del consentimiento de términos y condiciones (RGPD Art. 5.2).
     */
    @Transactional
    public TermsStatusResponse updateTermsAcceptance(Long userId, TermsAcceptanceRequest request) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        boolean accepted = Boolean.TRUE.equals(request.accepted());
        usuario.setTermsAccepted(accepted);

        if (accepted) {
            usuario.setTermsAcceptedAt(Instant.now());
            usuario.setTermsVersion(request.version() != null && !request.version().isBlank()
                    ? request.version().trim()
                    : "1.0");
            log.info("Usuario {} ha aceptado los términos y condiciones (versión {})", userId, usuario.getTermsVersion());
        } else {
            usuario.setTermsAcceptedAt(null);
            usuario.setTermsVersion(null);
            log.info("Usuario {} ha revocado o denegado los términos y condiciones", userId);
        }

        usuarioRepository.save(usuario);

        return new TermsStatusResponse(
                usuario.isTermsAccepted(),
                usuario.getTermsAcceptedAt(),
                usuario.getTermsVersion()
        );
    }

    /**
     * Ejerce el Derecho al Olvido (Art. 17 RGPD): elimina permanentemente al usuario,
     * revoca sus autorizaciones externas y borra en cascada todos sus datos asociados.
     */
    @Transactional
    public void deleteAccount(Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        // Revocar tokens OAuth2 externos vinculados a buzones de correo
        List<CuentaCorreo> cuentas = cuentaCorreoRepository.findByUsuarioIdOrderByFechaConexionDescIdDesc(userId);
        for (CuentaCorreo cuenta : cuentas) {
            if (cuenta.getProveedor() == ProveedorCorreo.GMAIL && cuenta.getRefreshTokenCifrado() != null) {
                try {
                    String token = tokenCipherService.decrypt(cuenta.getRefreshTokenCifrado());
                    gmailOAuthService.revocarToken(token);
                } catch (Exception e) {
                    log.warn("No se pudo revocar el token de Google para la cuenta {}: {}", cuenta.getId(), e.getMessage());
                }
            }
        }

        // El borrado en base de datos aplica ON DELETE CASCADE a cuenta_correo, candidatura, contacto, nota e historial_estado
        usuarioRepository.delete(usuario);
        log.info("Usuario {} ({}) y todos sus datos personales han sido eliminados por derecho al olvido (Art. 17 RGPD)",
                userId, usuario.getEmail());
    }
}
