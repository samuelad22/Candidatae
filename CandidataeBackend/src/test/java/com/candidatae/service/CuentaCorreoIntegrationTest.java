package com.candidatae.service;

import com.candidatae.CandidataeApplication;
import com.candidatae.domain.CuentaCorreo;
import com.candidatae.domain.EstadoConexion;
import com.candidatae.domain.ProveedorAuth;
import com.candidatae.domain.ProveedorCorreo;
import com.candidatae.domain.Usuario;
import com.candidatae.dto.CuentaCorreoDto;
import com.candidatae.exception.ApiException;
import com.candidatae.repository.CuentaCorreoRepository;
import com.candidatae.repository.UsuarioRepository;
import com.candidatae.service.CuentaCorreoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración contra un PostgreSQL real (Testcontainers).
 *
 * <p>Comprueba lo que los tests unitarios no pueden: que el esquema que crea Flyway
 * funciona, que los constraints de la base de datos son los esperados y, sobre todo,
 * que <strong>los datos de un usuario son invisibles para los demás</strong>.</p>
 */
@SpringBootTest(classes = CandidataeApplication.class)
@Testcontainers
@DisplayName("CuentaCorreo - integración con PostgreSQL real y aislamiento entre usuarios")
class CuentaCorreoIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private CuentaCorreoRepository cuentaCorreoRepository;

    @Autowired
    private CuentaCorreoService cuentaCorreoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Long anaId;
    private Long luisId;

    @BeforeEach
    @Transactional
    void crearUsuarios() {
        anaId = crearUsuario("ana@correo.com", "Ana");
        luisId = crearUsuario("luis@correo.com", "Luis");
    }

    private Long crearUsuario(String email, String nombre) {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .email(email)
                .name(nombre)
                .passwordHash("hash-de-prueba")
                .proveedorAuth(ProveedorAuth.LOCAL)
                .active(true)
                .build());
        return usuario.getId();
    }

    /**
     * Comprueba que la operación falla con el status HTTP esperado.
     *
     * <p>Con una única clase {@link ApiException} para todos los errores de negocio,
     * lo que distingue un error de otro es su status HTTP.</p>
     */
    private ApiException assertFallaCon(HttpStatus status, Executable operacion) {
        ApiException error = assertThrows(ApiException.class, operacion);
        assertEquals(status, error.getStatus());
        return error;
    }

    @Test
    @DisplayName("Test 1: cada usuario solo ve sus propias cuentas de correo")
    @Transactional
    void cadaUsuarioSoloVeSusCuentas() {
        cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("ana@gmail.com", ProveedorCorreo.GMAIL), anaId);
        cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("ana-trabajo@gmail.com", ProveedorCorreo.GMAIL), anaId);
        cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("luis@gmail.com", ProveedorCorreo.GMAIL), luisId);

        assertEquals(2, cuentaCorreoService.listar(anaId).size());
        assertEquals(1, cuentaCorreoService.listar(luisId).size());
        assertEquals("luis@gmail.com", cuentaCorreoService.listar(luisId).get(0).emailCuenta());
    }

    @Test
    @DisplayName("Test 2: un usuario no puede leer la cuenta de otro (404)")
    @Transactional
    void unUsuarioNoPuedeLeerLaCuentaDeOtro() {
        var cuenta = cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("ana@gmail.com", ProveedorCorreo.GMAIL), anaId);

        assertFallaCon(HttpStatus.NOT_FOUND,
                () -> cuentaCorreoService.obtener(cuenta.id(), luisId));
    }

    @Test
    @DisplayName("Test 3: un usuario no puede editar la cuenta de otro")
    @Transactional
    void unUsuarioNoPuedeEditarLaCuentaDeOtro() {
        var cuenta = cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("ana@gmail.com", ProveedorCorreo.GMAIL), anaId);

        assertFallaCon(HttpStatus.NOT_FOUND,
                () -> cuentaCorreoService.actualizar(cuenta.id(), luisId,
                        new CuentaCorreoDto.Update("secuestrada@gmail.com", null, null)));
    }

    @Test
    @DisplayName("Test 4: un usuario no puede desconectar la cuenta de otro")
    @Transactional
    void unUsuarioNoPuedeDesconectarLaCuentaDeOtro() {
        var cuenta = cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("ana@gmail.com", ProveedorCorreo.GMAIL), anaId);

        assertFallaCon(HttpStatus.NOT_FOUND,
                () -> cuentaCorreoService.desconectar(cuenta.id(), luisId));

        // La cuenta de Ana sigue intacta.
        assertEquals("PENDIENTE", cuentaCorreoService.obtener(cuenta.id(), anaId).estadoConexion());
    }

    @Test
    @DisplayName("Test 5: la misma bandeja puede estar vinculada por dos usuarios distintos")
    @Transactional
    void laMismaBandejaPuedeEstarEnDosUsuarios() {
        cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("compartido@gmail.com", ProveedorCorreo.GMAIL), anaId);
        cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("compartido@gmail.com", ProveedorCorreo.GMAIL), luisId);

        assertEquals(1, cuentaCorreoService.listar(anaId).size());
        assertEquals(1, cuentaCorreoService.listar(luisId).size());
    }

    @Test
    @DisplayName("Test 6: la misma bandeja NO se puede registrar dos veces por el mismo usuario")
    @Transactional
    void laMismaBandejaNoSeRepiteParaElMismoUsuario() {
        cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("repetida@gmail.com", ProveedorCorreo.GMAIL), anaId);

        assertThrows(DataIntegrityViolationException.class,
                () -> cuentaCorreoRepository.save(CuentaCorreo.builder()
                        .usuario(usuarioRepository.findById(anaId).orElseThrow())
                        .proveedor(ProveedorCorreo.GMAIL)
                        .emailCuenta("repetida@gmail.com")
                        .estadoConexion(EstadoConexion.PENDIENTE)
                        .build()));
    }

    @Test
    @DisplayName("Test 7: al borrar el usuario, sus cuentas de correo se borran también (CASCADE)")
    @Transactional
    void alBorrarElUsuarioSeBorraSusCuentas() {
        cuentaCorreoService.crear(
                new CuentaCorreoDto.Create("ana@gmail.com", ProveedorCorreo.GMAIL), anaId);

        // Flush previo: si insertamos la cuenta y borramos el usuario en el mismo
        // flush, Hibernate se queja de que la cuenta referencia un usuario "transitorio".
        cuentaCorreoRepository.flush();

        // Desvinculamos las entidades del contexto de persistencia: así no le damos a
        // Hibernate la oportunidad de "desenlazar" la cuenta, y el borrado en cascada
        // lo hace la base de datos (que es justo lo que queremos comprobar).
        entityManager.clear();

        usuarioRepository.deleteById(anaId);
        usuarioRepository.flush();

        List<CuentaCorreo> restantes = cuentaCorreoRepository.findByUsuarioIdOrderByFechaConexionDescIdDesc(anaId);
        assertTrue(restantes.isEmpty(), "Las cuentas de correo deberían haberse borrado en cascada");
    }

    @Test
    @DisplayName("Test 8: el refresh token se guarda CIFRADO en la base de datos")
    @Transactional
    void elRefreshTokenSeGuardaCifrado() {
        String tokenEnClaro = "ya29.a0AfH6SMB-refresh-token-en-claro";

        cuentaCorreoService.registrarVinculacion(anaId, "ana@gmail.com", tokenEnClaro);

        CuentaCorreo guardada = cuentaCorreoRepository
                .findByUsuarioIdAndEmailCuentaIgnoreCase(anaId, "ana@gmail.com")
                .orElseThrow();

        assertNotNull(guardada.getRefreshTokenCifrado());
        assertFalse(new String(guardada.getRefreshTokenCifrado(), StandardCharsets.UTF_8)
                .contains(tokenEnClaro), "El token se ha guardado en claro en la base de datos");
        assertEquals(EstadoConexion.ACTIVA, guardada.getEstadoConexion());
    }
}
