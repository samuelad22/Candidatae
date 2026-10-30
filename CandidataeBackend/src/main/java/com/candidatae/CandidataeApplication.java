package com.candidatae;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación Candidatae.
 *
 * <p>La clase vive en el paquete raíz {@code com.candidatae} a propósito: así
 * {@code @SpringBootApplication} escanea de forma automática <strong>todas</strong> las
 * capas ({@code controller}, {@code service}, {@code repository}, {@code domain},
 * {@code integration}, {@code scheduler}, {@code security} y {@code config}) sin
 * tener que declarar {@code scanBasePackages} a mano.</p>
 *
 * <p>Si algún día se moviera esta clase a un subpaquete, habría que acordarse de
 * widening el escaneo: Spring Data JPA también necesita encontrar las entidades
 * y los repositorios.</p>
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CandidataeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CandidataeApplication.class, args);
    }

}

