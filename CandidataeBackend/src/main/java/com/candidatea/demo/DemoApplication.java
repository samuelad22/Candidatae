package com.candidatea.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * Por defecto, @SpringBootApplication solo escanea el paquete donde está esta clase
 * (com.candidatea.demo) y sus subpaquetes. Como los módulos del monolito viven en
 * com.equipo.jobtracker.*, hay que indicárselo explícitamente:
 *
 *  - scanBasePackages: para que encuentre los @Component, @Service, @RestController, @Configuration...
 *  - @AutoConfigurationPackage: para que Spring Data JPA encuentre también las @Entity
 *    y los repositorios (JpaRepository) de ese paquete.
 */
@SpringBootApplication(scanBasePackages = {"com.candidatea.demo", "com.equipo.jobtracker"})
@AutoConfigurationPackage(basePackages = "com.equipo.jobtracker")
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
