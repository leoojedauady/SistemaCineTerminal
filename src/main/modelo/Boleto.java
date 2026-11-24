package modelo;

/**
 * Datos de la función y de la compra recargados desde sus repositorios.
 * @param funcion proyección registrada
 * @param compra compra con su precio histórico
 */
public record Boleto(Funcion funcion, Compra compra) {}