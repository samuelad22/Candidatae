package com.candidatae.dto;

import com.candidatae.domain.Contacto;

/**
 * Contacto profesional vinculado a una candidatura.
 */
public record ContactoResponse(
        Long id,
        String nombre,
        String email,
        String rol
) {
    /**
     * Mapea una entidad Contacto al record de respuesta.
     */
    public static ContactoResponse fromEntity(Contacto contacto) {
        if (contacto == null) {
            return null;
        }
        return new ContactoResponse(
                contacto.getId(),
                contacto.getNombre(),
                contacto.getEmail(),
                contacto.getRol()
        );
    }
}
