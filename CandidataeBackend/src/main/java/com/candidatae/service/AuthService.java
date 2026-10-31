package com.candidatae.service;

import com.candidatae.domain.ProveedorAuth;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.AuthDto;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.scheduler.UserLoggedInEvent;
import com.candidatae.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Servicio de negocio para operaciones de autenticación: registro y login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthDto.Response register(AuthDto.Register request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmail(email)) {
            throw ApiException.conflict("El email ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .email(email)
                .name(request.name().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .proveedorAuth(ProveedorAuth.LOCAL)
                .active(true)
                .termsAccepted(false)
                .build();

        usuarioRepository.save(usuario);

        return new AuthDto.Response(jwtService.generateToken(email), email, false);
    }

    public AuthDto.Response login(AuthDto.Login request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException e) {
            throw ApiException.unauthorized("Credenciales inválidas");
        }

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        boolean termsAccepted = usuario != null && usuario.isTermsAccepted();

        String token = jwtService.generateToken(email);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new UserLoggedInEvent(email));
        }
        return new AuthDto.Response(token, email, termsAccepted);
    }
}
