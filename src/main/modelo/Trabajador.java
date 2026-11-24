package modelo;

/**
 * Credencial persistida. Los valores binarios se representan en Base64.
 * @param usuario nombre de acceso
 * @param sal sal aleatoria
 * @param hash clave derivada
 */
public record Trabajador(String usuario, String sal, String hash) {}
