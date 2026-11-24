package modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Selección provisional de asientos y disponibilidad observada.
 * @param disponibilidad estado observado de la función
 * @param asientos lugares solicitados
 */
public record Seleccion(Disponibilidad disponibilidad, List<String> asientos) {
    /** Copia la selección provisional. */
    public Seleccion {
        asientos = List.copyOf(asientos);
    }

    /**
     * Calcula el importe que se muestra antes de confirmar.
     * @return total en pesos mexicanos
     */
    public BigDecimal total() {
        return disponibilidad.funcion().precio().multiply(BigDecimal.valueOf(asientos.size()));
    }
}
