package com.candidatae.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI (Swagger UI).
 *
 * <p>Sirve como contrato vivo de la API: el frontend puede consultarlo en
 * {@code http://localhost:8080/swagger-ui.html} para ver los endpoints, los DTOs
 * y los esquemas de seguridad sin tener que leer el código.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Esquema de seguridad "bearerAuth" (JWT).
     *
     * <p>Al declararlo aquí, todos los endpoints protegidos heredan el botón
     * "Authorize" de Swagger UI: basta con pegar el token que devuelve el login.</p>
     */
    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI candidataeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Candidatae API")
                        .version("0.0.1")
                        .description("API REST de Candidatae: centraliza el registro y el seguimiento "
                                + "de las solicitudes de empleo enviadas desde distintas plataformas.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
