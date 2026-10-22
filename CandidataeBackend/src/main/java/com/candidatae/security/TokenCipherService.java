package com.candidatae.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simétrico de secretos (tokens OAuth2 de Google) con AES-256-GCM.
 *
 * <p>Los {@code refresh_token} de Gmail dan acceso de lectura a todo el buzón del
 * usuario: son secretos de alto valor. Por eso <strong>nunca</strong> se guardan en
 * claro en la base de datos, sino cifrados con una clave que vive fuera de la BD
 * (variable de entorno).</p>
 *
 * <p>Formato del valor almacenado (columna BYTEA):</p>
 * <pre>
 *   [1 byte  ] versión del formato (vale 1)
 *   [12 bytes] IV aleatorio (GCM)
 *   [N bytes ] texto cifrado + tag de autenticación de 16 bytes
 * </pre>
 *
 * <p>El uso de GCM (mode authenticated) garantiza que alguien no pueda alterar el
 * ciphertext en la base de datos sin que el descifrado falle.</p>
 */
@Service
public class TokenCipherService {

    private static final Logger log = LoggerFactory.getLogger(TokenCipherService.class);

    /** Versión actual del formato de datos cifrados. */
    private static final byte FORMAT_VERSION = 1;

    /** Longitud del IV en bytes (96 bits, el tamaño recomendado para GCM). */
    private static final int IV_LENGTH = 12;

    /** Longitud de la clave AES-256 en bytes. */
    private static final int KEY_LENGTH = 32;

    /** Longitud del tag de autenticación GCM en bits. */
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenCipherService(@Value("${gmail.token-encryption-key}") String base64Key) {
        this.key = new SecretKeySpec(decodeKey(base64Key), "AES");
    }

    /**
     * Cifra un texto en claro.
     *
     * @param plaintext texto a cifrar (por ejemplo, el refresh token).
     * @return bytes listos para persistir en la columna BYTEA.
     */
    public byte[] encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }

        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return ByteBuffer.allocate(1 + iv.length + ciphertext.length)
                    .put(FORMAT_VERSION)
                    .put(iv)
                    .put(ciphertext)
                    .array();
        } catch (GeneralSecurityException e) {
            // Nunca devolvemos el detalle técnico al usuario: lo registramos y relanzamos.
            log.error("No se ha podido cifrar el token: {}", e.getMessage());
            throw new IllegalStateException("Error al cifrar el token de correo", e);
        }
    }

    /**
     * Descifra un texto previamente cifrado con {@link #encrypt(String)}.
     *
     * @param encrypted bytes leídos de la base de datos.
     * @return texto en claro, o {@code null} si la entrada era {@code null}.
     */
    public String decrypt(byte[] encrypted) {
        if (encrypted == null) {
            return null;
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(encrypted);
            byte version = buffer.get();
            if (version != FORMAT_VERSION) {
                throw new IllegalStateException("Formato de token cifrado no soportado: " + version);
            }

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            log.error("No se ha podido descifrar el token: {}", e.getMessage());
            throw new IllegalStateException("Error al descifrar el token de correo", e);
        }
    }

    /**
     * Decodifica la clave de la variable de entorno y comprueba que tiene el tamaño correcto.
     *
     * @param base64Key clave en Base64.
     * @return 32 bytes para AES-256.
     */
    private byte[] decodeKey(String base64Key) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "gmail.token-encryption-key no es una cadena Base64 válida", e);
        }

        if (decoded.length != KEY_LENGTH) {
            throw new IllegalStateException(
                    "gmail.token-encryption-key debe tener " + KEY_LENGTH + " bytes (AES-256), pero tiene "
                            + decoded.length + ". Genera una con: openssl rand -base64 32");
        }
        return decoded;
    }
}
