package com.equipo.jobtracker.user.service;

import com.equipo.jobtracker.user.model.User;
import com.equipo.jobtracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * API pública interna del módulo "user" para consultas de otros módulos (ej. "application").
 */
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    /**
     * Busca y devuelve el ID del usuario a partir de su email.
     *
     * @param email email del usuario autenticado.
     * @return ID numérico del usuario.
     * @throws ResponseStatusException 401 UNAUTHORIZED si el usuario no existe.
     */
    @Transactional(readOnly = true)
    public Long findUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(normalizedEmail)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }
}
