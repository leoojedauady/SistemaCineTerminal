package seguridad;

import modelo.Trabajador;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Deriva y verifica credenciales mediante PBKDF2 con una sal por cuenta. */
public final class Clave {
    private static final int ITERACIONES = 150_000;
    private Clave() {}

    /**
     * Crea una credencial para la carga inicial.
     * @param usuario nombre de acceso
     * @param contrasena contraseña temporal en memoria
     * @return credencial codificada en Base64
     */
    public static Trabajador crear(String usuario, char[] contrasena) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        Base64.Encoder base64 = Base64.getEncoder();
        return new Trabajador(usuario, base64.encodeToString(sal),
                base64.encodeToString(derivar(contrasena, sal)));
    }

    /**
     * Verifica una contraseña contra el hash de la cuenta.
     * @param contrasena entrada temporal
     * @param trabajador credencial almacenada
     * @return si coincide
     */
    public static boolean verificar(char[] contrasena, Trabajador trabajador) {
        Base64.Decoder base64 = Base64.getDecoder();
        return MessageDigest.isEqual(base64.decode(trabajador.hash()),
                derivar(contrasena, base64.decode(trabajador.sal())));
    }

    private static byte[] derivar(char[] contrasena, byte[] sal) {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, sal, ITERACIONES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo verificar la credencial.", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
