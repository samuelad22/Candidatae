package com.candidatae.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TokenCipherService - cifrado AES-256-GCM de los tokens de Gmail")
class TokenCipherServiceTest {

    private final TokenCipherService cipherService = new TokenCipherService(claveBase64(1));

    /**
     * Genera una clave determinista de 32 bytes (AES-256) a partir de un "seed".
     * En producción la clave sale de la variable de entorno
     * GMAIL_TOKEN_ENCRYPTION_KEY y nunca está en el repositorio.
     */
    private static String claveBase64(int seed) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) seed);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    @DisplayName("Test 1: cifrar y descifrar devuelve el texto original")
    void cifrarYDescifrarDevuelveElOriginal() {
        String token = "ya29.a0AfH6SMB-virtual-refresh-token";

        byte[] cifrado = cipherService.encrypt(token);

        assertEquals(token, cipherService.decrypt(cifrado));
    }

    @Test
    @DisplayName("Test 2: el texto cifrado NO contiene el token en claro")
    void elCifradoNoContieneElTokenEnClaro() {
        String token = "ya29.a0AfH6SMB-refresh-token-secreto";

        byte[] cifrado = cipherService.encrypt(token);

        String comoTexto = new String(cifrado, StandardCharsets.ISO_8859_1);
        assertFalse(comoTexto.contains(token), "El token se ha filtrado en el texto cifrado");
        assertFalse(comoTexto.contains("refresh-token"));
    }

    @Test
    @DisplayName("Test 3: dos cifrados del mismo token son distintos (IV aleatorio)")
    void dosCifradosDelMismoTokenSonDistintos() {
        String token = "mismo-token";

        byte[] primero = cipherService.encrypt(token);
        byte[] segundo = cipherService.encrypt(token);

        assertFalse(Arrays.equals(primero, segundo),
                "El IV es aleatorio: dos cifrados nunca deben ser idénticos");
        // ...pero ambos descifran al mismo texto.
        assertEquals(token, cipherService.decrypt(primero));
        assertEquals(token, cipherService.decrypt(segundo));
    }

    @Test
    @DisplayName("Test 4: null se traduce en null al cifrar y descifrar")
    void nullSeTraduceEnNull() {
        assertNull(cipherService.encrypt(null));
        assertNull(cipherService.decrypt(null));
    }

    @Test
    @DisplayName("Test 5: el token alterado no se puede descifrar (GCM detecta la manipulación)")
    void unTokenAlteradoNoSePuedeDescifrar() {
        byte[] cifrado = cipherService.encrypt("token-original");
        cifrado[cifrado.length - 1] = (byte) (cifrado[cifrado.length - 1] ^ 0xFF);

        assertThrows(RuntimeException.class, () -> cipherService.decrypt(cifrado));
    }

    @Test
    @DisplayName("Test 6: una clave que no es Base64 válida falla al arrancar")
    void unaClaveInvalidaFallaAlArrancar() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new TokenCipherService("no-es-base64-!!!"));

        assertTrue(error.getMessage().contains("Base64"));
    }

    @Test
    @DisplayName("Test 7: una clave que no tiene 32 bytes falla al arrancar")
    void unaClaveConLongitudIncorrectaFallaAlArrancar() {
        String claveCorta = Base64.getEncoder().encodeToString("corto".getBytes(StandardCharsets.UTF_8));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new TokenCipherService(claveCorta));

        assertTrue(error.getMessage().contains("32"));
    }

    @Test
    @DisplayName("Test 8: el formato cifrado es version(1) + IV(12) + texto + tag GCM(16)")
    void elFormatoCifradoEsElEsperado() {
        String token = "token";
        byte[] cifrado = cipherService.encrypt(token);

        assertEquals(1, cifrado[0], "El primer byte es la versión del formato");
        assertEquals(1 + 12 + token.getBytes(StandardCharsets.UTF_8).length + 16, cifrado.length);
    }

    @Test
    @DisplayName("Test 9: una clave distinta no puede descifrar el token")
    void otraClaveNoPuedeDescifrarElToken() {
        byte[] cifrado = cipherService.encrypt("token-secreto");
        TokenCipherService otroServicio = new TokenCipherService(claveBase64(2));

        assertThrows(RuntimeException.class, () -> otroServicio.decrypt(cifrado));
    }
}
