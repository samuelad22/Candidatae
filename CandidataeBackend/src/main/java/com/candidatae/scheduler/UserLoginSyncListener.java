package com.candidatae.scheduler;

import com.candidatae.service.EmailSyncService;
import com.candidatae.service.UsuarioQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronous listener that triggers email synchronization on the user's
 * first login of the day without blocking the HTTP login response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserLoginSyncListener {

    private final UsuarioQueryService usuarioQueryService;
    private final EmailSyncService emailSyncService;

    @Async
    @EventListener
    public void onUserLoggedIn(UserLoggedInEvent event) {
        if (event == null || event.email() == null || event.email().isBlank()) {
            return;
        }
        try {
            Long userId = usuarioQueryService.findUserIdByEmail(event.email());
            emailSyncService.syncOnFirstDailyLogin(userId);
        } catch (Exception e) {
            log.warn("Could not complete background login sync for {}: {}", event.email(), e.getMessage());
        }
    }
}

