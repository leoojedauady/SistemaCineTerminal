package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de compra con precio histórico y todos sus lugares.
 * @param folio identificador único
 * @param funcionId identificador de la función
 * @param fecha momento de compra
 * @param asientos identificadores de los asientos
 * @param precio precio unitario en MXN al comprar
 * @param vigente si ocupa sus asientos
 */
public record Compra(long folio, long funcionId, LocalDateTime fecha, List<String> asientos,
                     BigDecimal precio, boolean vigente) {
    /** Copia la selección para mantener inmutable la compra. */
    public Compra {
        asientos = List.copyOf(asientos);
    }

    /**
     * Calcula el importe histórico.
     * @return precio unitario multiplicado por el número de asientos
     */
    public BigDecimal total() {
        return precio.multiply(BigDecimal.valueOf(asientos.size()));
    }
}
