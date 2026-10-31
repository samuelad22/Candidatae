package com.candidatae.controller;

import com.candidatae.dto.TermsDto.TermsAcceptanceRequest;
import com.candidatae.dto.TermsDto.TermsStatusResponse;
import com.candidatae.security.CurrentUserProvider;
import com.candidatae.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la gestión de datos del usuario autenticado, consentimiento y derecho al olvido (RGPD).
 */
@Tag(name = "Usuarios", description = "Endpoints de perfil de usuario, consentimiento legal y derecho al olvido (RGPD / LOPDGDD)")
@RestController
@RequestMapping({"/api/users", "/api/usuarios"})
@RequiredArgsConstructor
public class UserController {

    private final UsuarioService usuarioService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "Consultar estado de términos y condiciones",
            description = "Devuelve si el usuario autenticado ha aceptado los términos vigentes y la fecha/versión de consentimiento.")
    @GetMapping("/terms")
    public ResponseEntity<TermsStatusResponse> getTermsStatus(@AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserProvider.requireUserId(principal);
        return ResponseEntity.ok(usuarioService.getTermsStatus(userId));
    }

    @Operation(summary = "Registrar consentimiento de términos y condiciones",
            description = "Registra en base de datos la aceptación auditada de los términos legales conforme al Art. 5.2 del RGPD.")
    @PostMapping("/terms")
    public ResponseEntity<TermsStatusResponse> updateTermsAcceptance(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody TermsAcceptanceRequest request
    ) {
        Long userId = currentUserProvider.requireUserId(principal);
        return ResponseEntity.ok(usuarioService.updateTermsAcceptance(userId, request));
    }

    @Operation(summary = "Ejercer derecho al olvido (supresión de cuenta)",
            description = "Elimina permanentemente la cuenta del usuario, revoca tokens y borra en cascada todos sus datos asociados (RGPD Art. 17).")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserDetails principal) {
        Long userId = currentUserProvider.requireUserId(principal);
        usuarioService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
