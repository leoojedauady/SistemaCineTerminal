package modelo;

/**
 * Película del catálogo. Su identidad se conserva si más adelante se marca inactiva.
 * @param id identificador
 * @param titulo nombre mostrado
 * @param duracion minutos de proyección
 * @param clasificacion clasificación de audiencia
 * @param activa si puede programarse
 */
public record Pelicula(long id, String titulo, int duracion, String clasificacion, boolean activa) {}
