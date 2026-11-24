package persistencia;

import modelo.Compra;
import modelo.Funcion;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Puerto que encapsula la revalidación y el registro indivisible de una compra. */
public interface RepositorioCompras {
    /**
     * Recupera los lugares asignados a compras vigentes.
     * @param funcionId función consultada
     * @return lugares actualmente ocupados
     */
    Set<String> ocupados(long funcionId);

    /**
     * Recupera una compra histórica, vigente o cancelada.
     * @param folio identificador de compra
     * @return registro si existe
     */
    Optional<Compra> buscar(long folio);

    /**
     * Revalida el horario y la ocupación dentro de la operación de escritura y asigna el folio.
     * @param funcion función elegida
     * @param asientos selección validada estructuralmente
     * @param reloj referencia temporal del prototipo
     * @return folio persistido
     */
    long registrar(Funcion funcion, List<String> asientos, Clock reloj);
}
