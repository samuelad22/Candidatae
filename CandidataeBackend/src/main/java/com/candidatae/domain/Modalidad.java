package com.candidatae.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Modalidad de trabajo de una oferta o candidatura de empleo.
 */
public enum Modalidad {
    PRESENCIAL,
    HIBRIDO,
    REMOTO;

    @JsonCreator
    public static Modalidad fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        for (Modalidad m : values()) {
            if (m.name().equals(normalized)) {
                return m;
            }
        }
        throw new IllegalArgumentException(value.trim());
    }
}
