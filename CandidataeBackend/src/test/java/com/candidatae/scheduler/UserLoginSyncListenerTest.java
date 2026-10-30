package com.candidatae.scheduler;

import com.candidatae.service.EmailSyncService;
import com.candidatae.service.UsuarioQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLoginSyncListener - unit tests for login event synchronization")
class UserLoginSyncListenerTest {

    @Mock
    private UsuarioQueryService usuarioQueryService;

    @Mock
    private EmailSyncService emailSyncService;

    @InjectMocks
    private UserLoginSyncListener listener;

    @Test
    @DisplayName("onUserLoggedIn() resolves userId and triggers syncOnFirstDailyLogin()")
    void triggersSyncOnFirstDailyLogin() {
        when(usuarioQueryService.findUserIdByEmail("ana@correo.com")).thenReturn(1L);

        listener.onUserLoggedIn(new UserLoggedInEvent("ana@correo.com"));

        verify(usuarioQueryService).findUserIdByEmail("ana@correo.com");
        verify(emailSyncService).syncOnFirstDailyLogin(1L);
    }

    @Test
    @DisplayName("onUserLoggedIn() ignores blank email events")
    void ignoresBlankEvents() {
        listener.onUserLoggedIn(new UserLoggedInEvent("  "));

        verifyNoInteractions(usuarioQueryService, emailSyncService);
    }
}

