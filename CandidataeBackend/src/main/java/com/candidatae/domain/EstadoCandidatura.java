package com.candidatae.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Estados posibles en el ciclo de vida de una candidatura de empleo.
 */
public enum EstadoCandidatura {
    ENVIADA,
    EN_PROCESO,
    ENTREVISTA,
    OFERTA,
    RECHAZADA,
    DESCARTADA;

    @JsonCreator
    public static EstadoCandidatura fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        for (EstadoCandidatura estado : values()) {
            if (estado.name().equals(normalized)) {
                return estado;
            }
        }
        throw new IllegalArgumentException(value.trim());
    }
}
