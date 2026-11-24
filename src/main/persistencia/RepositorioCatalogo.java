package persistencia;

import modelo.Funcion;
import modelo.Pelicula;
import java.util.List;

/** Puerto de persistencia de la programación y del catálogo de películas. */
public interface RepositorioCatalogo {
    /**
     * Recupera la programación con su película y sala.
     * @return funciones registradas
     */
    List<Funcion> funciones();

    /**
     * Asigna un identificador y almacena una película activa.
     * @param titulo nombre validado
     * @param duracion duración validada
     * @param clasificacion clasificación validada
     * @return película registrada
     */
    Pelicula agregarPelicula(String titulo, int duracion, String clasificacion);
}
