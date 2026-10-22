package com.candidatae.repository;

import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.EstadoConexion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link CuentaCorreo}.
 *
 * <p><strong>Regla de oro de esta tabla:</strong> ninguna consulta puede devolver datos
 * sin filtrar por {@code usuario_id}. Por eso todos los métodos de lectura exigen el
 * {@code usuarioId} como primer parámetro, y {@link #findById(Long)} queda
 * bloqueado para que nadie lo use por accidente.</p>
 *
 * <p>Motivo: los datos de una bandeja de correo son privados. Si una consulta se
 * cuela sin filtro, un usuario podría ver (o borrar) la cuenta de otro solo cambiando
 * el {@code id} de la URL.</p>
 */
@Repository
public interface CuentaCorreoRepository extends JpaRepository<CuentaCorreo, Long> {

    /**
     * Lista las cuentas de correo de un usuario, de la más reciente a la más antigua.
     */
    List<CuentaCorreo> findByUsuarioIdOrderByFechaConexionDescIdDesc(Long usuarioId);

    /**
     * Busca una cuenta por id <strong>perteneciendo al usuario indicado</strong>.
     *
     * @return vacío si la cuenta no existe o es de otro usuario (en ambos casos se
     *         responde 404, para no revelar que el recurso existe).
     */
    Optional<CuentaCorreo> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Busca la cuenta de un usuario con ese email de bandeja (sin distinguir mayúsculas).
     */
    Optional<CuentaCorreo> findByUsuarioIdAndEmailCuentaIgnoreCase(Long usuarioId, String emailCuenta);

    /**
     * Cuentas del usuario en los estados indicados.
     *
     * <p><strong>Todavía sin uso:</strong> la consumirá el scheduler de sincronización
     * de la Fase 3, para localizar las cuentas {@code ACTIVA} a las que hay que leer
     * el buzón. Se deja declarada y documentada, no olvidada por descuido: cuando
     * exista el {@code @Scheduled}, esta será su consulta de entrada.</p>
     */
    List<CuentaCorreo> findByUsuarioIdAndEstadoConexionIn(Long usuarioId, Collection<EstadoConexion> estados);

    /**
     * Bloqueado a propósito: usar siempre {@link #findByIdAndUsuarioId(Long, Long)}.
     *
     * <p>Spring Data genera este método a partir de {@code JpaRepository}. Lo sobrescribimos
     * para que su uso falle de forma ruidosa en lugar de devolver datos de otro usuario.</p>
     */
    @Override
    default Optional<CuentaCorreo> findById(Long id) {
        throw new UnsupportedOperationException(
                "findById(Long) está bloqueado por aislamiento de datos: "
                        + "usa findByIdAndUsuarioId(id, usuarioId)");
    }
}
