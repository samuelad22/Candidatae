package com.candidatae.security;

import com.candidatae.exception.ApiException;
import com.candidatae.service.UsuarioQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Resuelve el identificador del usuario autenticado a partir del principal de Spring Security.
 *
 * <p>En Candidatae el identificador de sesión es el <em>email</em> (es el "subject" del JWT),
 * pero las tablas de negocio usan el {@code id} numérico. Este componente centraliza ese
 * "traductor" para que los controladores no repitan la lógica y, sobre todo, para que
 * <strong>toda</strong> consulta se filtre por el usuario correcto.</p>
 *
 * <p>Ejemplo de uso en un controlador:</p>
 * <pre>
 * &#64;GetMapping
 * public List&lt;X&gt; listar(&#64;AuthenticationPrincipal UserDetails principal) {
 *     Long usuarioId = currentUserProvider.requireUserId(principal);
 *     return servicio.listar(usuarioId);
 * }
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UsuarioQueryService usuarioQueryService;

    /**
     * Devuelve el {@code id} del usuario autenticado.
     *
     * @param principal principal de Spring Security (puede ser {@code null} si no hay sesión).
     * @return id numérico del usuario.
     * @throws ApiException 401 si no hay usuario autenticado.
     */
    public Long requireUserId(UserDetails principal) {
        if (principal == null || principal.getUsername() == null || principal.getUsername().isBlank()) {
            throw ApiException.unauthorized("Usuario no autenticado");
        }
        return requireUserId(principal.getUsername());
    }

    /**
     * Devuelve el {@code id} del usuario a partir de su email.
     *
     * @param email email del usuario (el "subject" del JWT).
     * @return id numérico del usuario.
     * @throws ApiException 401 si el email no corresponde a un usuario activo.
     */
    public Long requireUserId(String email) {
        return usuarioQueryService.findUserIdByEmail(email);
    }

    /**
     * Variante que lee directamente del contexto de seguridad.
     *
     * @return id numérico del usuario autenticado.
     * @throws ApiException 401 si no hay usuario autenticado.
     */
    public Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw ApiException.unauthorized("Usuario no autenticado");
        }
        return requireUserId(authentication.getName());
    }
}
