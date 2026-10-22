package com.candidatae.service;

import com.candidatae.domain.Usuario;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Servicio de consulta de usuarios.
 *
 * <p>Su propósito es resolver la referencia al usuario autenticado: en Candidatae la
 * sesión se identifica por el <em>email</em> (es el "subject" del JWT), pero las
 * tablas de negocio (por ejemplo {@code cuenta_correo}) usan el {@code id} numérico.
 * Este servicio es el único punto que traduce de uno a otro.</p>
 *
 * <p>Es de solo lectura: no modifica usuarios. Para crear o modificar usuarios está
 * {@link AuthService}.</p>
 */
@Service
@RequiredArgsConstructor
public class UsuarioQueryService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Busca y devuelve el ID del usuario a partir de su email.
     *
     * @param email email del usuario autenticado.
     * @return ID numérico del usuario.
     * @throws ApiException 401 si el email no corresponde a un usuario activo.
     */
    @Transactional(readOnly = true)
    public Long findUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw ApiException.unauthorized("Usuario no autenticado");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return usuarioRepository.findByEmail(normalizedEmail)
                .filter(Usuario::isActive)
                .map(Usuario::getId)
                .orElseThrow(() -> ApiException.unauthorized("Usuario no autenticado o inactivo"));
    }
}
