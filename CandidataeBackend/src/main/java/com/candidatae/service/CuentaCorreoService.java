package com.candidatae.service;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.EstadoConexion;
import com.candidatae.domain.ProveedorCorreo;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CuentaCorreoDto;
import com.candidatae.exception.ApiException;
import com.candidatae.integration.gmail.GmailOAuthService;
import com.candidatae.repository.CuentaCorreoRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.security.TokenCipherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Lógica de negocio de las cuentas de correo del usuario.
 *
 * <p><strong>Aislamiento de datos:</strong> todos los métodos reciben el
 * {@code usuarioId} del usuario autenticado y todas las consultas se filtran por él.
 * Una cuenta que pertenece a otro usuario simplemente no existe para este usuario
 * (se responde 404, no 403, para no filtrar información).</p>
 *
 * <p><strong>Desacoplamiento de Google:</strong> este servicio no sabe cómo funciona
 * OAuth2. Solo conoce la operación de negocio "revoca este token en el proveedor",
 * que implementa {@link GmailOAuthService}. Si mañana se soporta Outlook, el cambio
 * no toca este archivo.</p>
 *
 * <p><strong>Secretos:</strong> el {@code refresh_token} se cifra con
 * {@link TokenCipherService} antes de tocar la base de datos y nunca sale por la API.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaCorreoService {

    private final CuentaCorreoRepository cuentaCorreoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenCipherService tokenCipherService;
    private final GmailOAuthService gmailOAuthService;

    /**
     * Lista las cuentas de correo del usuario.
     */
    @Transactional(readOnly = true)
    public List<CuentaCorreoDto.Response> listar(Long usuarioId) {
        return cuentaCorreoRepository.findByUsuarioIdOrderByFechaConexionDescIdDesc(usuarioId)
                .stream()
                .map(CuentaCorreoDto.Response::from)
                .toList();
    }

    /**
     * Devuelve una cuenta concreta del usuario.
     *
     * @throws ApiException 404 si no existe o no es del usuario.
     */
    @Transactional(readOnly = true)
    public CuentaCorreoDto.Response obtener(Long id, Long usuarioId) {
        return CuentaCorreoDto.Response.from(buscar(id, usuarioId));
    }

    /**
     * Alta manual de una cuenta de correo, sin pasar por el flujo OAuth de Google.
     *
     * <p>La cuenta nace en estado {@code PENDIENTE}: el usuario ha declarado su bandeja
     * pero todavía no ha autorizado el acceso. Sirve para que el registro exista desde
     * el primer momento, aunque la vinculación automática tarde o falle.</p>
     *
     * <p>Si la bandeja ya estaba registrada y estaba desconectada, se "reutiliza" la
     * fila (se pone a {@code PENDIENTE} y se limpia la fecha de desconexión) en lugar de
     * crear un duplicado: así el usuario puede volver a añadir una bandeja que él mismo
     * desconectó, sin perder el histórico de esa cuenta.</p>
     *
     * @throws ApiException 409 si el usuario ya tiene esa cuenta conectada.
     */
    @Transactional
    public CuentaCorreoDto.Response crear(CuentaCorreoDto.Create dto, Long usuarioId) {
        String email = normalizar(dto.emailCuenta());

        CuentaCorreo existente = cuentaCorreoRepository
                .findByUsuarioIdAndEmailCuentaIgnoreCase(usuarioId, email)
                .orElse(null);

        if (existente != null) {
            if (existente.getEstadoConexion() != EstadoConexion.DESCONECTADA) {
                throw ApiException.conflict("Ya tienes registrada la cuenta de correo " + email);
            }

            // Reutilizamos la fila desconectada en lugar de duplicarla.
            if (dto.proveedor() != null) {
                existente.setProveedor(dto.proveedor());
            }
            existente.setEstadoConexion(EstadoConexion.PENDIENTE);
            existente.setFechaDesconexion(null);
            existente.setCursorSincronizacion(null);
            return CuentaCorreoDto.Response.from(cuentaCorreoRepository.save(existente));
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        CuentaCorreo cuenta = CuentaCorreo.builder()
                .usuario(usuario)
                .proveedor(dto.proveedor() != null ? dto.proveedor() : ProveedorCorreo.GMAIL)
                .emailCuenta(email)
                .estadoConexion(EstadoConexion.PENDIENTE)
                .build();

        return CuentaCorreoDto.Response.from(cuentaCorreoRepository.save(cuenta));
    }

    /**
     * Edita una cuenta de correo del usuario.
     *
     * <p>No permite tocar el refresh token (eso solo pasa por el flujo OAuth) ni
     * poner la cuenta en ACTIVA a mano (el estado ACTIVA significa "tenemos un token
     * válido guardado", y eso solo se puede afirmar tras un intercambio real).</p>
     *
     * @throws ApiException 404 si no existe o no es del usuario.
     * @throws ApiException 400 si se intenta un cambio de estado no permitido o cambiar el email de una cuenta ACTIVA.
     * @throws ApiException 409 si el nuevo email ya está en uso por otra cuenta.
     */
    @Transactional
    public CuentaCorreoDto.Response actualizar(Long id, Long usuarioId, CuentaCorreoDto.Update dto) {
        CuentaCorreo cuenta = buscar(id, usuarioId);

        if (dto.emailCuenta() != null && !dto.emailCuenta().isBlank()) {
            String email = normalizar(dto.emailCuenta());
            if (!email.equalsIgnoreCase(cuenta.getEmailCuenta())) {
                boolean yaLaTiene = cuentaCorreoRepository
                        .findByUsuarioIdAndEmailCuentaIgnoreCase(usuarioId, email)
                        .map(existente -> !existente.getId().equals(cuenta.getId()))
                        .orElse(false);

                if (yaLaTiene) {
                    throw ApiException.conflict("Ya tienes registrada la cuenta de correo " + email);
                }

                // Al cambiar el email de una cuenta ACTIVA, las credenciales OAuth del buzón
                // anterior quedan invalidadas y la cuenta pasa a requerir nueva autorización.
                if (cuenta.getEstadoConexion() == EstadoConexion.ACTIVA) {
                    cuenta.setRefreshTokenCifrado(null);
                    cuenta.setCursorSincronizacion(null);
            cuenta.setLastUid(null);
            cuenta.setUidValidity(null);
            cuenta.setLastSyncAt(null);
                    cuenta.setEstadoConexion(EstadoConexion.PENDIENTE);
                }
                cuenta.setEmailCuenta(email);
            }
        }

        if (dto.estadoConexion() != null) {
            EstadoConexion nuevo = dto.estadoConexion();
            if (nuevo != EstadoConexion.PENDIENTE && nuevo != EstadoConexion.EXPIRADA) {
                throw ApiException.badRequest(
                        "Solo se puede marcar la cuenta como PENDIENTE o EXPIRADA. "
                                + "Para activarla hay que vincular de nuevo con Google, "
                                + "y para desconectarla está el endpoint de borrado.");
            }
            cuenta.setEstadoConexion(nuevo);
        }

        if (Boolean.TRUE.equals(dto.resetCursor())) {
            cuenta.setCursorSincronizacion(null);
            cuenta.setLastUid(null);
            cuenta.setUidValidity(null);
            cuenta.setLastSyncAt(null);
        }

        return CuentaCorreoDto.Response.from(cuentaCorreoRepository.save(cuenta));
    }

    /**
     * Desconecta una cuenta de correo.
     *
     * <p>La desconexión es <strong>lógica</strong>: se revoca el token en Google, se
     * borra el token cifrado y se marca la cuenta como {@code DESCONECTADA}, pero la fila
     * se conserva para no perder el histórico de sincronizaciones ni de correos
     * procesados que la referencian.</p>
     *
     * @throws ApiException 404 si no existe o no es del usuario.
     */
    @Transactional
    public void desconectar(Long id, Long usuarioId) {
        CuentaCorreo cuenta = buscar(id, usuarioId);

        if (cuenta.getRefreshTokenCifrado() != null) {
            try {
                if (cuenta.getProveedor() == ProveedorCorreo.GMAIL) {
                    // Revocamos en Google (best effort) para que el acceso muera de inmediato.
                    String tokenEnClaro = tokenCipherService.decrypt(cuenta.getRefreshTokenCifrado());
                    gmailOAuthService.revocarToken(tokenEnClaro);
                }
            } catch (Exception e) {
                log.warn("No se ha podido descifrar o revocar el token en Google para la cuenta {}: {}. Continuando con la desconexión local.", id, e.getMessage());
            }
        }

        cuenta.setRefreshTokenCifrado(null);
        cuenta.setEstadoConexion(EstadoConexion.DESCONECTADA);
        cuenta.setFechaDesconexion(Instant.now());

        cuentaCorreoRepository.save(cuenta);
        log.info("Cuenta de correo {} desconectada por el usuario {}", id, usuarioId);
    }

    /**
     * Marca el estado de conexión de una cuenta.
     *
     * <p><strong>Uso interno</strong> (scheduler de sincronización, Fase 3). No está
     * expuesto por la API, y por eso puede fijar cualquier estado —incluidos
     * {@code ACTIVA} y {@code CON_ERROR}—, a diferencia de la edición manual
     * ({@link #actualizar}), que es más restrictiva.</p>
     *
     * @throws ApiException 404 si no existe o no es del usuario.
     */
    @Transactional
    public void marcarEstado(Long id, Long usuarioId, EstadoConexion estado) {
        CuentaCorreo cuenta = buscar(id, usuarioId);

        cuenta.setEstadoConexion(estado);
        cuentaCorreoRepository.save(cuenta);
    }

    /**
     * Guarda el cursor de sincronización: la marca de hasta dónde se ha leído el buzón.
     *
     * <p><strong>Uso interno</strong> (scheduler de sincronización, Fase 3). El cursor
     * es lo que hace que la siguiente lectura sea incremental: sin él, la cuenta
     * vuelve a leerse entera desde el principio.</p>
     *
     * @param cursor último punto leído del buzón (por ejemplo, el {@code historyId} de
     *               Gmail). Un {@code null} reinicia la lectura desde cero.
     * @throws ApiException 404 si no existe o no es del usuario.
     */
    @Transactional
    public void guardarCursor(Long id, Long usuarioId, String cursor) {
        CuentaCorreo cuenta = buscar(id, usuarioId);

        cuenta.setCursorSincronizacion(cursor);
        cuentaCorreoRepository.save(cuenta);
    }

    /**
     * Registra (o refresca) una vinculación completada por el flujo OAuth de Google.
     *
     * <p>Si la bandeja ya estaba registrada, se actualiza con el token nuevo: queda
     * {@code ACTIVA}, con fecha de conexión nueva y sin cursor (para que la primera
     * sincronización lea el buzón desde el principio).</p>
     *
     * <p>No se revoca el token anterior al sobrescribirlo: Google suele devolver el
     * mismo refresh token cuando la app ya tenía acceso, y revocarlo dejaría sin
     * credenciales la cuenta que acabamos de vincular.</p>
     */
    @Transactional
    public CuentaCorreoDto.Response registrarVinculacion(Long usuarioId, String email, String refreshToken) {
        return registrarVinculacion(usuarioId, email, refreshToken, ProveedorCorreo.GMAIL);
    }

    @Transactional
    public CuentaCorreoDto.Response registrarVinculacion(Long usuarioId, String email, String refreshToken, ProveedorCorreo proveedor) {
        String emailNormalizado = normalizar(email);

        CuentaCorreo cuenta = cuentaCorreoRepository
                .findByUsuarioIdAndEmailCuentaIgnoreCase(usuarioId, emailNormalizado)
                .orElse(null);

        if (cuenta == null) {
            // Solo lo buscamos si la cuenta es nueva: si ya existe, el usuario ya está
            // en la fila y no hace falta una consulta extra.
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

            cuenta = CuentaCorreo.builder()
                    .usuario(usuario)
                    .proveedor(proveedor != null ? proveedor : ProveedorCorreo.GMAIL)
                    .emailCuenta(emailNormalizado)
                    .build();
        }

        cuenta.setProveedor(proveedor != null ? proveedor : ProveedorCorreo.GMAIL);
        cuenta.setEmailCuenta(emailNormalizado);
        cuenta.setRefreshTokenCifrado(tokenCipherService.encrypt(refreshToken));
        cuenta.setEstadoConexion(EstadoConexion.ACTIVA);
        cuenta.setFechaConexion(Instant.now());
        cuenta.setFechaDesconexion(null);
        cuenta.setCursorSincronizacion(null);

        return CuentaCorreoDto.Response.from(cuentaCorreoRepository.save(cuenta));
    }

    /**
     * Busca una cuenta exigiendo que pertenezca al usuario.
     *
     * @throws ApiException 404 si no existe o no es del usuario.
     */
    private CuentaCorreo buscar(Long id, Long usuarioId) {
        return cuentaCorreoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> ApiException.notFound("Cuenta de correo no encontrada"));
    }

    /**
     * Normaliza un email para que las comparaciones y el constraint único
     * {@code (usuario_id, email_cuenta)} se comporten igual en toda la aplicación.
     */
    private String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
