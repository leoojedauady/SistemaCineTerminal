package modelo;

/**
 * Sala rectangular cuyos asientos se identifican con una letra y una columna.
 * @param numero número de sala
 * @param filas número de filas
 * @param columnas número de columnas
 */
public record Sala(int numero, int filas, int columnas) {
    /**
     * Comprueba que el identificador corresponde a un lugar de la sala.
     * @param asiento identificador en mayúsculas, por ejemplo B2
     * @return si el identificador existe
     */
    public boolean contiene(String asiento) {
        return asiento.matches("[A-Z][1-9][0-9]?")
                && asiento.charAt(0) - 'A' < filas
                && Integer.parseInt(asiento.substring(1)) <= columnas;
    }
}
