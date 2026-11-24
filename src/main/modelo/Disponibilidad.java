package modelo;
import java.util.Set;

/**
 * Vista de una función con la ocupación observada en el momento de la consulta.
 * @param funcion función consultada
 * @param ocupados asientos con compras vigentes
 */
public record Disponibilidad(Funcion funcion, Set<String> ocupados) {
    /** Copia el conjunto de ocupación para preservar la vista. */
    public Disponibilidad {
        ocupados = Set.copyOf(ocupados);
    }

    /**
     * Cuenta los lugares disponibles.
     * @return capacidad de la sala menos lugares ocupados
     */
    public int libres() {
        return funcion.sala().filas() * funcion.sala().columnas() - ocupados.size();
    }
}
