package persistencia;

import modelo.Trabajador;
import java.util.Optional;

/** Puerto de lectura de credenciales del personal. */
public interface RepositorioTrabajadores {
    /**
     * Busca una cuenta por nombre.
     * @param usuario nombre de acceso
     * @return credencial si existe
     */
    Optional<Trabajador> buscarTrabajador(String usuario);
}
