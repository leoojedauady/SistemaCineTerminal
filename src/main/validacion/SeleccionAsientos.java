package validacion;

import modelo.Sala;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Interpreta identificadores de asientos y valida su pertenencia a una sala. */
public final class SeleccionAsientos {
    private SeleccionAsientos() {}

    /**
     * Normaliza una lista separada por comas y rechaza lugares inexistentes o repetidos.
     * @param texto selección escrita por el usuario
     * @param sala sala de la función
     * @return lista inmutable en el orden solicitado
     * @throws IllegalArgumentException si la selección es inválida
     */
    public static List<String> interpretar(String texto, Sala sala) {
        List<String> asientos = Arrays.stream(texto.split(",", -1))
                .map(String::trim)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .toList();
        if (asientos.stream().anyMatch(s -> !sala.contiene(s))) {
            throw new IllegalArgumentException("Asiento inválido. Escribe lugares del mapa separados por comas.");
        }
        if (new HashSet<>(asientos).size() != asientos.size()) {
            throw new IllegalArgumentException("Hay asientos repetidos. Escribe cada lugar una sola vez.");
        }
        return asientos;
    }
}
