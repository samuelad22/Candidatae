package com.candidatae.config;

import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.JsonAccessDeniedHandler;
import com.candidatae.security.JsonAuthenticationEntryPoint;
import com.candidatae.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Locale;

/**
 * Configuración central de Spring Security para la aplicación.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
                                                   JsonAccessDeniedHandler jsonAccessDeniedHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Registro y login: son los únicos puntos públicos de la API
                        // de autenticación (el usuario aún no tiene sesión al entrar).
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        // Callback de la vinculación OAuth2 de Gmail: lo llama Google, no el
                        // usuario, así que no lleva JWT. La protección la aporta el "state"
                        // firmado que valida el controlador (ver GmailOAuthService).
                        .requestMatchers("/api/email-accounts/gmail/callback").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Documentación de la API.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization", "Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Construye el {@link UserDetailsService} que usa Spring Security.
     *
     * <p>El "nombre" de usuario en Candidatae es el email: es lo que viaja en el
     * subject del JWT y lo que identifica al usuario aunque acceda con Google o
     * LinkedIn en el futuro.</p>
     */
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return email -> usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(usuario -> User.withUsername(usuario.getEmail())
                        .password(usuario.getPasswordHash() != null ? usuario.getPasswordHash() : "")
                        .disabled(!usuario.isActive())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
