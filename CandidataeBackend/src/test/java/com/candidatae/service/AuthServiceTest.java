package com.candidatae.service;

import com.candidatae.domain.ProveedorAuth;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.AuthDto;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.scheduler.UserLoggedInEvent;
import com.candidatae.security.JwtService;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - unit tests")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Test 1: successful registration -> hashes password, saves usuario and returns token")
        void successfulRegistration() {
            AuthDto.Register request = new AuthDto.Register("  Ana@Correo.com ", "password123", " Ana ");

            when(usuarioRepository.existsByEmail("ana@correo.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("MOCK_BCRYPT_HASH");
            when(jwtService.generateToken("ana@correo.com")).thenReturn("mock.jwt.token");

            AuthDto.Response response = authService.register(request);

            assertThat(response.token()).isEqualTo("mock.jwt.token");
            assertThat(response.email()).isEqualTo("ana@correo.com");

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario saved = captor.getValue();

            assertThat(saved.getEmail()).isEqualTo("ana@correo.com");
            assertThat(saved.getName()).isEqualTo("Ana");
            assertThat(saved.getPasswordHash())
                    .isEqualTo("MOCK_BCRYPT_HASH")
                    .isNotEqualTo("password123");
            assertThat(saved.getProveedorAuth()).isEqualTo(ProveedorAuth.LOCAL);
            assertThat(saved.isActive()).isTrue();

            verify(passwordEncoder).encode("password123");
            verify(jwtService).generateToken("ana@correo.com");
        }

        @Test
        @DisplayName("Test 2: duplicate email -> throws 409 CONFLICT and does not save")
        void duplicateEmailThrowsConflict() {
            AuthDto.Register request = new AuthDto.Register("ana@correo.com", "password123", "Ana");
            when(usuarioRepository.existsByEmail("ana@correo.com")).thenReturn(true);

            ApiException ex = assertThrows(
                    ApiException.class,
                    () -> authService.register(request));

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);

            verify(usuarioRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
            verify(jwtService, never()).generateToken(anyString());
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Test 3: successful login -> delegates to AuthenticationManager and generates token")
        void successfulLogin() {
            AuthDto.Login request = new AuthDto.Login("Ana@Correo.com", "password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mock(Authentication.class));
            when(jwtService.generateToken("ana@correo.com")).thenReturn("mock.jwt.token");

            AuthDto.Response response = authService.login(request);

            assertThat(response.token()).isEqualTo("mock.jwt.token");
            assertThat(response.email()).isEqualTo("ana@correo.com");

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getPrincipal()).isEqualTo("ana@correo.com");
            assertThat(captor.getValue().getCredentials()).isEqualTo("password123");
            verify(eventPublisher).publishEvent(new UserLoggedInEvent("ana@correo.com"));
        }

        @Test
        @DisplayName("Test extra: invalid credentials -> throws 401 and does not generate token")
        void invalidCredentialsThrowsUnauthorized() {
            AuthDto.Login request = new AuthDto.Login("ana@correo.com", "wrongpassword");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            ApiException ex = assertThrows(
                    ApiException.class,
                    () -> authService.login(request));

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ex.getMessage()).isEqualTo("Credenciales inválidas");
            verify(jwtService, never()).generateToken(anyString());
        }
    }
}
