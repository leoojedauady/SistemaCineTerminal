package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección programada con un precio único por asiento.
 * @param id identificador
 * @param pelicula película proyectada
 * @param sala sala asignada
 * @param inicio fecha y hora local de inicio
 * @param fin fecha y hora local de término
 * @param precio importe en MXN por asiento
 */
public record Funcion(long id, Pelicula pelicula, Sala sala, LocalDateTime inicio,
                      LocalDateTime fin, BigDecimal precio) {}
