package com.equipo.jobtracker.user;

import com.equipo.jobtracker.user.dto.AuthResponseDto;
import com.equipo.jobtracker.user.dto.LoginRequestDto;
import com.equipo.jobtracker.user.dto.RegisterRequestDto;
import com.equipo.jobtracker.user.model.User;
import com.equipo.jobtracker.user.repository.UserRepository;
import com.equipo.jobtracker.user.security.JwtService;
import com.equipo.jobtracker.user.service.AuthService;
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
import org.springframework.web.server.ResponseStatusException;

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
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Test 1: successful registration -> hashes password, saves user and returns token")
        void successfulRegistration() {
            RegisterRequestDto request = new RegisterRequestDto("  Ana@Correo.com ", "password123", " Ana ");

            when(userRepository.existsByEmail("ana@correo.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("MOCK_BCRYPT_HASH");
            when(jwtService.generateToken("ana@correo.com")).thenReturn("mock.jwt.token");

            AuthResponseDto response = authService.register(request);

            assertThat(response.token()).isEqualTo("mock.jwt.token");
            assertThat(response.email()).isEqualTo("ana@correo.com");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getEmail()).isEqualTo("ana@correo.com");
            assertThat(saved.getName()).isEqualTo("Ana");
            assertThat(saved.getPasswordHash())
                    .isEqualTo("MOCK_BCRYPT_HASH")
                    .isNotEqualTo("password123");
            assertThat(saved.getAuthProvider()).isEqualTo("LOCAL");
            assertThat(saved.isActive()).isTrue();

            verify(passwordEncoder).encode("password123");
            verify(jwtService).generateToken("ana@correo.com");
        }

        @Test
        @DisplayName("Test 2: duplicate email -> throws 409 CONFLICT and does not save")
        void duplicateEmailThrowsConflict() {
            RegisterRequestDto request = new RegisterRequestDto("ana@correo.com", "password123", "Ana");
            when(userRepository.existsByEmail("ana@correo.com")).thenReturn(true);

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.register(request));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            verify(userRepository, never()).save(any());
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
            LoginRequestDto request = new LoginRequestDto("Ana@Correo.com", "password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mock(Authentication.class));
            when(jwtService.generateToken("ana@correo.com")).thenReturn("mock.jwt.token");

            AuthResponseDto response = authService.login(request);

            assertThat(response.token()).isEqualTo("mock.jwt.token");
            assertThat(response.email()).isEqualTo("ana@correo.com");

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getPrincipal()).isEqualTo("ana@correo.com");
            assertThat(captor.getValue().getCredentials()).isEqualTo("password123");
        }

        @Test
        @DisplayName("Test extra: invalid credentials -> throws 401 and does not generate token")
        void invalidCredentialsThrowsUnauthorized() {
            LoginRequestDto request = new LoginRequestDto("ana@correo.com", "wrongpassword");
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.login(request));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ex.getReason()).isEqualTo("Credenciales inválidas");
            verify(jwtService, never()).generateToken(anyString());
        }
    }
}
