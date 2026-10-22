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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CuentaCorreoService - reglas de negocio del CRUD de cuentas de correo")
class CuentaCorreoServiceTest {

    private static final Long USUARIO_ANA = 1L;
    private static final Long USUARIO_LUIS = 2L;

    @Mock
    private CuentaCorreoRepository cuentaCorreoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenCipherService tokenCipherService;

    @Mock
    private GmailOAuthService gmailOAuthService;

    @InjectMocks
    private CuentaCorreoService service;

    private Usuario usuario(Long id, String email) {
        return Usuario.builder().id(id).email(email).name("Usuario").active(true).build();
    }

    private CuentaCorreo cuenta(Long id, Long usuarioId, String email, EstadoConexion estado) {
        return CuentaCorreo.builder()
                .id(id)
                .usuario(usuario(usuarioId, "dueño@correo.com"))
                .proveedor(ProveedorCorreo.GMAIL)
                .emailCuenta(email)
                .estadoConexion(estado)
                .build();
    }

    /**
     * Comprueba que la operación falla con el status HTTP esperado.
     *
     * <p>Con una única clase {@link ApiException} para todos los errores de negocio,
     * lo que distingue un error de otro es su status HTTP: por eso se comprueba
     * aquí, y no solo el tipo de la excepción.</p>
     */
    private ApiException assertFallaCon(HttpStatus status, Executable operacion) {
        ApiException error = assertThrows(ApiException.class, operacion);
        assertEquals(status, error.getStatus());
        return error;
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("Test 1: devuelve solo las cuentas del usuario indicado")
        void devuelveSoloLasCuentasDelUsuario() {
            CuentaCorreo cuenta = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            when(cuentaCorreoRepository.findByUsuarioIdOrderByFechaConexionDescIdDesc(USUARIO_ANA))
                    .thenReturn(List.of(cuenta));

            List<CuentaCorreoDto.Response> resultado = service.listar(USUARIO_ANA);

            assertEquals(1, resultado.size());
            assertEquals("ana@gmail.com", resultado.get(0).emailCuenta());
            verify(cuentaCorreoRepository).findByUsuarioIdOrderByFechaConexionDescIdDesc(USUARIO_ANA);
        }

        @Test
        @DisplayName("Test 2: un usuario sin cuentas recibe una lista vacía (no un error)")
        void usuarioSinCuentasRecibeListaVacia() {
            when(cuentaCorreoRepository.findByUsuarioIdOrderByFechaConexionDescIdDesc(USUARIO_LUIS))
                    .thenReturn(List.of());

            assertTrue(service.listar(USUARIO_LUIS).isEmpty());
        }
    }

    @Nested
    @DisplayName("obtener")
    class Obtener {

        @Test
        @DisplayName("Test 3: una cuenta ajena se responde 404 (no 403, para no filtrar información)")
        void unaCuentaAjenaSeResponde404() {
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_LUIS)).thenReturn(Optional.empty());

            ApiException error = assertFallaCon(HttpStatus.NOT_FOUND,
                    () -> service.obtener(1L, USUARIO_LUIS));

            assertEquals("Cuenta de correo no encontrada", error.getMessage());
        }

        @Test
        @DisplayName("Test 4: devuelve la cuenta cuando pertenece al usuario")
        void devuelveLaCuentaDelUsuario() {
            CuentaCorreo cuenta = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(cuenta));

            CuentaCorreoDto.Response dto = service.obtener(1L, USUARIO_ANA);

            assertEquals(1L, dto.id());
            assertTrue(dto.activa());
        }
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("Test 5: normaliza el email a minúsculas y nace en estado PENDIENTE")
        void normalizaElEmailYNaceEnPendiente() {
            when(cuentaCorreoRepository.findByUsuarioIdAndEmailCuentaIgnoreCase(USUARIO_ANA, "ana@gmail.com"))
                    .thenReturn(Optional.empty());
            when(usuarioRepository.findById(USUARIO_ANA)).thenReturn(Optional.of(usuario(USUARIO_ANA, "ana@correo.com")));

            ArgumentCaptor<CuentaCorreo> captor = ArgumentCaptor.forClass(CuentaCorreo.class);
            when(cuentaCorreoRepository.save(any(CuentaCorreo.class))).thenAnswer(inv -> inv.getArgument(0));

            CuentaCorreoDto.Response dto = service.crear(
                    new CuentaCorreoDto.Create("  ANA@Gmail.com ", ProveedorCorreo.GMAIL), USUARIO_ANA);

            verify(cuentaCorreoRepository).save(captor.capture());
            assertEquals("ana@gmail.com", captor.getValue().getEmailCuenta());
            assertEquals("PENDIENTE", dto.estadoConexion());
            assertFalse(dto.activa());
            assertNull(dto.fechaConexion());
        }

        @Test
        @DisplayName("Test 6: si la cuenta ya existe y está activa, responde 409")
        void siLaCuentaYaEstaActivaResponde409() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            when(cuentaCorreoRepository.findByUsuarioIdAndEmailCuentaIgnoreCase(USUARIO_ANA, "ana@gmail.com"))
                    .thenReturn(Optional.of(existente));

            assertFallaCon(HttpStatus.CONFLICT, () -> service.crear(
                    new CuentaCorreoDto.Create("ana@gmail.com", ProveedorCorreo.GMAIL), USUARIO_ANA));

            verify(cuentaCorreoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Test 7: si la cuenta estaba desconectada, se reutiliza la fila (no se duplica)")
        void siEstabaDesconectadaSeReutilizaLaFila() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.DESCONECTADA);
            existente.setFechaDesconexion(Instant.now());
            when(cuentaCorreoRepository.findByUsuarioIdAndEmailCuentaIgnoreCase(USUARIO_ANA, "ana@gmail.com"))
                    .thenReturn(Optional.of(existente));
            when(cuentaCorreoRepository.save(any(CuentaCorreo.class))).thenAnswer(inv -> inv.getArgument(0));

            CuentaCorreoDto.Response dto = service.crear(
                    new CuentaCorreoDto.Create("ana@gmail.com", ProveedorCorreo.GMAIL), USUARIO_ANA);

            assertEquals(1L, dto.id());
            assertEquals("PENDIENTE", dto.estadoConexion());
            assertNull(dto.fechaDesconexion());
            verify(cuentaCorreoRepository).save(existente);
            verify(usuarioRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        @DisplayName("Test 8: no permite poner la cuenta en ACTIVA a mano")
        void noPermitePonerActivaAMano() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.PENDIENTE);
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(existente));

            ApiException error = assertFallaCon(HttpStatus.BAD_REQUEST,
                    () -> service.actualizar(1L, USUARIO_ANA, new CuentaCorreoDto.Update(null, EstadoConexion.ACTIVA, null)));

            assertTrue(error.getMessage().contains("Google"));
            verify(cuentaCorreoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Test 9: permite marcar la cuenta como EXPIRADA")
        void permiteMarcarComoExpirada() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(existente));
            when(cuentaCorreoRepository.save(any(CuentaCorreo.class))).thenAnswer(inv -> inv.getArgument(0));

            CuentaCorreoDto.Response dto = service.actualizar(1L, USUARIO_ANA,
                    new CuentaCorreoDto.Update(null, EstadoConexion.EXPIRADA, null));

            assertEquals("EXPIRADA", dto.estadoConexion());
        }

        @Test
        @DisplayName("Test 10: resetCursor borra el cursor de sincronización")
        void resetCursorBorraElCursor() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            existente.setCursorSincronizacion("123456789");
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(existente));
            when(cuentaCorreoRepository.save(any(CuentaCorreo.class))).thenAnswer(inv -> inv.getArgument(0));

            service.actualizar(1L, USUARIO_ANA, new CuentaCorreoDto.Update(null, null, true));

            assertNull(existente.getCursorSincronizacion());
        }

        @Test
        @DisplayName("Test 11: cambiar a un email que ya usa otra cuenta propia -> 409")
        void cambiarAUnEmailYaUsadoResponde409() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            CuentaCorreo otra = cuenta(2L, USUARIO_ANA, "trabajo@gmail.com", EstadoConexion.ACTIVA);
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(existente));
            when(cuentaCorreoRepository.findByUsuarioIdAndEmailCuentaIgnoreCase(USUARIO_ANA, "trabajo@gmail.com"))
                    .thenReturn(Optional.of(otra));

            assertFallaCon(HttpStatus.CONFLICT, () -> service.actualizar(1L, USUARIO_ANA,
                    new CuentaCorreoDto.Update("trabajo@gmail.com", null, null)));

            verify(cuentaCorreoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Test 12: una cuenta ajena se responde 404")
        void unaCuentaAjenaSeResponde404() {
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_LUIS)).thenReturn(Optional.empty());

            assertFallaCon(HttpStatus.NOT_FOUND, () -> service.actualizar(1L, USUARIO_LUIS,
                    new CuentaCorreoDto.Update("otro@gmail.com", null, null)));
        }
    }

    @Nested
    @DisplayName("desconectar")
    class Desconectar {

        @Test
        @DisplayName("Test 13: marca DESCONECTADA, borra el token y pone la fecha de desconexión")
        void marcaDesconectadaYBorraElToken() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            existente.setRefreshTokenCifrado("token-cifrado".getBytes());
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(existente));
            when(tokenCipherService.decrypt(any(byte[].class))).thenReturn("refresh-token-en-claro");
            when(cuentaCorreoRepository.save(any(CuentaCorreo.class))).thenAnswer(inv -> inv.getArgument(0));

            service.desconectar(1L, USUARIO_ANA);

            assertEquals(EstadoConexion.DESCONECTADA, existente.getEstadoConexion());
            assertNull(existente.getRefreshTokenCifrado());
            assertNotNull(existente.getFechaDesconexion());
        }

        @Test
        @DisplayName("Test 14: revoca el token en Google antes de borrar el token cifrado")
        void revocaElTokenEnGoogle() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            existente.setRefreshTokenCifrado("token-cifrado".getBytes());
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(existente));
            when(tokenCipherService.decrypt(any(byte[].class))).thenReturn("refresh-token-en-claro");

            service.desconectar(1L, USUARIO_ANA);

            verify(gmailOAuthService).revocarToken("refresh-token-en-claro");
        }

        @Test
        @DisplayName("Test 15: una cuenta ajena se responde 404 y no se toca nada")
        void unaCuentaAjenaSeResponde404YTocaNada() {
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_LUIS)).thenReturn(Optional.empty());

            assertFallaCon(HttpStatus.NOT_FOUND, () -> service.desconectar(1L, USUARIO_LUIS));

            verify(cuentaCorreoRepository, never()).save(any());
            verify(gmailOAuthService, never()).revocarToken(any());
        }
    }

    @Nested
    @DisplayName("registrarVinculacion")
    class RegistrarVinculacion {

        @Test
        @DisplayName("Test 16: cifra el token, marca ACTIVA y limpia el cursor")
        void cifraElTokenYMarcaActiva() {
            when(cuentaCorreoRepository.findByUsuarioIdAndEmailCuentaIgnoreCase(USUARIO_ANA, "ana@gmail.com"))
                    .thenReturn(Optional.empty());
            when(usuarioRepository.findById(USUARIO_ANA)).thenReturn(Optional.of(usuario(USUARIO_ANA, "ana@correo.com")));
            when(tokenCipherService.encrypt("refresh-token")).thenReturn("cifrado".getBytes());
            when(cuentaCorreoRepository.save(any(CuentaCorreo.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<CuentaCorreo> captor = ArgumentCaptor.forClass(CuentaCorreo.class);
            CuentaCorreoDto.Response dto = service.registrarVinculacion(USUARIO_ANA, "Ana@Gmail.com", "refresh-token");

            verify(cuentaCorreoRepository).save(captor.capture());
            verify(tokenCipherService).encrypt("refresh-token");

            assertEquals("ACTIVA", dto.estadoConexion());
            assertTrue(dto.activa());
            assertNotNull(dto.fechaConexion());
            assertNull(dto.fechaDesconexion());
            assertEquals("ana@gmail.com", captor.getValue().getEmailCuenta());
            assertEquals(ProveedorCorreo.GMAIL, captor.getValue().getProveedor());
        }

        @Test
        @DisplayName("Test 17: si la bandeja ya estaba registrada, se actualiza en vez de duplicar")
        void siYaEstabaRegistradaSeActualiza() {
            CuentaCorreo existente = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.EXPIRADA);
            existente.setCursorSincronizacion("cursor-viejo");
            when(cuentaCorreoRepository.findByUsuarioIdAndEmailCuentaIgnoreCase(USUARIO_ANA, "ana@gmail.com"))
                    .thenReturn(Optional.of(existente));
            when(tokenCipherService.encrypt("refresh-token")).thenReturn("cifrado".getBytes());
            when(cuentaCorreoRepository.save(any(CuentaCorreo.class))).thenAnswer(inv -> inv.getArgument(0));

            CuentaCorreoDto.Response dto = service.registrarVinculacion(USUARIO_ANA, "ana@gmail.com", "refresh-token");

            assertEquals(1L, dto.id());
            assertEquals("ACTIVA", dto.estadoConexion());
            assertNull(existente.getCursorSincronizacion());
            verify(cuentaCorreoRepository, never()).save(argThat((CuentaCorreo c) -> c.getId() == null));
        }
    }

    @Nested
    @DisplayName("la respuesta pública nunca filtra el token")
    class LaRespuestaPublicaNuncaFiltraElToken {

        @Test
        @DisplayName("Test 18: el DTO de salida no tiene ningún campo de token")
        void elDtoDeSalidaNoTieneCamposDeToken() {
            List<String> campos = Arrays.stream(CuentaCorreoDto.Response.class.getRecordComponents())
                    .map(java.lang.reflect.RecordComponent::getName)
                    .toList();

            assertFalse(campos.contains("refreshToken"));
            assertFalse(campos.contains("refreshTokenCifrado"));
            assertTrue(campos.contains("emailCuenta"));
        }

        @Test
        @DisplayName("Test 19: el toString de la entidad no incluye el token cifrado")
        void elToStringDeLaEntidadNoIncluyeElToken() {
            CuentaCorreo cuenta = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            cuenta.setRefreshTokenCifrado("secreto".getBytes());

            assertFalse(cuenta.toString().contains("secreto"),
                    "El token cifrado no debe aparecer en el toString (acaba en los logs)");
        }
    }

    @Nested
    @DisplayName("el repositorio no permite consultas sin usuario")
    class ElRepositorioNoPermiteConsultasSinUsuario {

        @Test
        @DisplayName("Test 20: findById(Long) está bloqueado")
        void findByIdEstaBloqueado() {
            // Con CALLS_REAL_METHODS se ejecuta la implementación por defecto del
            // método findById, que es la que queremos verificar.
            CuentaCorreoRepository repositorioReal = Mockito.mock(CuentaCorreoRepository.class, Answers.CALLS_REAL_METHODS);

            UnsupportedOperationException error = assertThrows(UnsupportedOperationException.class,
                    () -> repositorioReal.findById(1L));

            assertTrue(error.getMessage().contains("findByIdAndUsuarioId"));
        }
    }

    @Nested
    @DisplayName("operaciones internas del scheduler (Fase 3)")
    class OperacionesInternasDelScheduler {

        @Test
        @DisplayName("Test 21: marcarEstado cambia el estado de la cuenta")
        void marcarEstadoCambiaElEstado() {
            CuentaCorreo cuenta = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(cuenta));

            service.marcarEstado(1L, USUARIO_ANA, EstadoConexion.CON_ERROR);

            assertEquals(EstadoConexion.CON_ERROR, cuenta.getEstadoConexion());
            verify(cuentaCorreoRepository).save(cuenta);
        }

        @Test
        @DisplayName("Test 22: marcarEstado sobre una cuenta ajena se responde 404")
        void marcarEstadoDeCuentaAjenaSeResponde404() {
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_LUIS)).thenReturn(Optional.empty());

            assertFallaCon(HttpStatus.NOT_FOUND,
                    () -> service.marcarEstado(1L, USUARIO_LUIS, EstadoConexion.EXPIRADA));

            verify(cuentaCorreoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Test 23: guardarCursor guarda el punto de lectura del buzón")
        void guardarCursorGuardaElCursor() {
            CuentaCorreo cuenta = cuenta(1L, USUARIO_ANA, "ana@gmail.com", EstadoConexion.ACTIVA);
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_ANA)).thenReturn(Optional.of(cuenta));

            service.guardarCursor(1L, USUARIO_ANA, "history-42");

            assertEquals("history-42", cuenta.getCursorSincronizacion());
            verify(cuentaCorreoRepository).save(cuenta);
        }

        @Test
        @DisplayName("Test 24: guardarCursor sobre una cuenta ajena se responde 404")
        void guardarCursorDeCuentaAjenaSeResponde404() {
            when(cuentaCorreoRepository.findByIdAndUsuarioId(1L, USUARIO_LUIS)).thenReturn(Optional.empty());

            assertFallaCon(HttpStatus.NOT_FOUND,
                    () -> service.guardarCursor(1L, USUARIO_LUIS, "history-42"));

            verify(cuentaCorreoRepository, never()).save(any());
        }
    }
}