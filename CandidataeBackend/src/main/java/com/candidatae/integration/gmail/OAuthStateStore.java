package com.candidatae.integration.gmail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén en memoria de los "state" pendientes de la vinculación OAuth2.
 *
 * <p>Cuando el usuario pide autorizar Gmail, el backend genera un valor aleatorio
 * ({@code state}) y lo asocia a su {@code usuario_id}. Cuando Google redirige de vuelta,
 * el callback entrega ese {@code state}: si coincide con uno pendiente, sabemos a qué
 * usuario pertenece la vinculación y que nadie ha manipulado la URL.</p>
 *
 * <p>Cada {@code state} es de un solo uso ({@link #consume(String)} lo elimina) y caduca
 * a los pocos minutos, para que un enlace robado no sirva dentro de una hora.</p>
 *
 * <p><strong>Límite conocido:</strong> el almacén es local a la instancia de la aplicación.
 * Si Candidatae se despliega con más de una instancia, un callback puede acabar en una
 * instancia distinta de la que generó el {@code state}. Para despliegues multi-instancia
 * habría que mover este estado a Redis o a una tabla de la base de datos. En el Sprint 1
 * la aplicación se despliega en una sola instancia, por lo que se mantiene en memoria.</p>
 */
@Component
public class OAuthStateStore {

    private static final int STATE_BYTES = 32;

    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final long ttlSeconds;

    public OAuthStateStore(@Value("${app.gmail.state-ttl-seconds:600}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Registra una vinculación pendiente y devuelve el {@code state} que hay que
     * incluir en la URL de autorización.
     *
     * @param usuarioId usuario que ha iniciado el flujo.
     * @return valor aleatorio de un solo uso.
     */
    public String register(Long usuarioId) {
        purgeExpired();

        byte[] randomBytes = new byte[STATE_BYTES];
        secureRandom.nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        pendingLinks.put(state, new PendingLink(usuarioId, Instant.now().plusSeconds(ttlSeconds)));
        return state;
    }

    /**
     * Consume un {@code state}: lo devuelve si era válido y lo elimina del almacén.
     *
     * @param state valor recibido en el callback.
     * @return el usuario al que pertenece la vinculación, o vacío si el state no es válido.
     */
    public Optional<Long> consume(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }

        PendingLink link = pendingLinks.remove(state);
        if (link == null) {
            return Optional.empty();
        }
        if (link.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(link.usuarioId());
    }

    /**
     * Elimina las vinculaciones caducadas para que el mapa no crezca sin control.
     */
    private void purgeExpired() {
        Instant now = Instant.now();
        pendingLinks.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    /**
     * Vinculación pendiente: a qué usuario pertenece y hasta cuándo es válida.
     */
    private record PendingLink(Long usuarioId, Instant expiresAt) {
    }
}
