package com.candidatae.controller;

import com.candidatae.dto.AuthDto;
import com.candidatae.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de autenticación de Candidatae ({@code /api/auth}).
 *
 * <p>La autenticación web (este controlador) y la conexión de bandejas de correo
 * ({@link CuentaCorreoController}) son <strong>procesos independientes</strong>: este
 * identifica al usuario en Candidatae; el otro le da acceso de lectura a su buzón.</p>
 *
 * <p>Ambos endpoints son públicos: quien se autentica no lo está todavía.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Alta de un usuario nuevo con email y contraseña.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthDto.Response> registro(@Valid @RequestBody AuthDto.Register request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Inicio de sesión con email y contraseña. Devuelve el JWT de sesión.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthDto.Response> login(@Valid @RequestBody AuthDto.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
