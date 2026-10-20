package com.equipo.jobtracker.user.service;

import com.equipo.jobtracker.user.dto.AuthResponseDto;
import com.equipo.jobtracker.user.dto.LoginRequestDto;
import com.equipo.jobtracker.user.dto.RegisterRequestDto;
import com.equipo.jobtracker.user.model.User;
import com.equipo.jobtracker.user.repository.UserRepository;
import com.equipo.jobtracker.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Servicio de negocio para operaciones de autenticación: registro y login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        User user = User.builder()
                .email(email)
                .name(request.name().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .authProvider("LOCAL")
                .active(true)
                .build();

        userRepository.save(user);

        return new AuthResponseDto(jwtService.generateToken(email), email);
    }

    public AuthResponseDto login(LoginRequestDto request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        return new AuthResponseDto(jwtService.generateToken(email), email);
    }
}
