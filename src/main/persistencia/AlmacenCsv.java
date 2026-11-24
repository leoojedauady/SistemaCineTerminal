package persistencia;

import modelo.Compra;
import modelo.Funcion;
import modelo.Pelicula;
import modelo.Sala;
import modelo.Trabajador;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Adaptador CSV de uso exclusivo. Las operaciones sincronizadas comparten un
 * bloqueo de directorio.
 */
public final class AlmacenCsv implements RepositorioCatalogo, RepositorioCompras,
        RepositorioTrabajadores, AutoCloseable {
    private static final String PELICULAS = "id,titulo,duracion,clasificacion,activa";
    private static final String SALAS = "numero,filas,columnas";
    private static final String FUNCIONES = "id,pelicula,sala,inicio,fin,precio";
    private static final String COMPRAS = "folio,funcion,fecha,asientos,precio,vigente";
    private static final String TRABAJADORES = "usuario,sal,hash";
    private final Path carpeta;
    private final FileChannel canal;
    private final FileLock bloqueo;

    /**
     * Abre los datos y adquiere exclusividad hasta que termina la sesión de
     * consola.
     * 
     * @param carpeta directorio existente con los archivos CSV
     * @throws IOException si faltan los datos o el directorio está en uso
     */
    public AlmacenCsv(Path carpeta) throws IOException {
        this.carpeta = carpeta.toRealPath();
        canal = FileChannel.open(this.carpeta.resolve("cine.lock"), CREATE, WRITE);
        try {
            bloqueo = canal.tryLock();
            if (bloqueo == null) {
                throw new IOException("Los datos están en uso por otra terminal.");
            }
        } catch (OverlappingFileLockException e) {
            canal.close();
            throw new IOException("Los datos están en uso por otra terminal.", e);
        } catch (IOException | RuntimeException e) {
            canal.close();
            throw e;
        }
    }

    /**
     * Inicializa archivos de demostración en un directorio de preparación vacío.
     * 
     * @param carpeta    directorio temporal de preparación
     * @param inicio     hora de la función inicial
     * @param trabajador credencial de muestra
     * @throws IOException si no se pueden escribir los archivos
     */
    public static void prepararDemo(Path carpeta, LocalDateTime inicio, Trabajador trabajador) throws IOException {
        ArchivoCsv.guardar(carpeta.resolve("peliculas.csv"), PELICULAS,
                List.of(ArchivoCsv.fila(1, "Viaje al cenote", 105, "A", true)));
        ArchivoCsv.guardar(carpeta.resolve("salas.csv"), SALAS, List.of(ArchivoCsv.fila(1, 4, 8)));
        ArchivoCsv.guardar(carpeta.resolve("funciones.csv"), FUNCIONES,
                List.of(ArchivoCsv.fila(1, 1, 1, inicio, inicio.plusMinutes(105), "70.00")));
        ArchivoCsv.guardar(carpeta.resolve("compras.csv"), COMPRAS, List.of());
        ArchivoCsv.guardar(carpeta.resolve("trabajadores.csv"), TRABAJADORES,
                List.of(ArchivoCsv.fila(trabajador.usuario(), trabajador.sal(), trabajador.hash())));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<Funcion> funciones() {
        Map<Long, Pelicula> peliculas = peliculas().stream()
                .collect(Collectors.toMap(Pelicula::id, Function.identity()));
        Map<Integer, Sala> salas = leer("salas.csv", SALAS,
                r -> new Sala(entero(r, 0), entero(r, 1), entero(r, 2))).stream()
                .collect(Collectors.toMap(Sala::numero, Function.identity()));
        return leer("funciones.csv", FUNCIONES, r -> new Funcion(largo(r, 0),
                referencia(peliculas, largo(r, 1)), referencia(salas, entero(r, 2)),
                LocalDateTime.parse(r.get(3)), LocalDateTime.parse(r.get(4)), new BigDecimal(r.get(5))));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Pelicula agregarPelicula(String titulo, int duracion, String clasificacion) {
        List<Pelicula> existentes = peliculas();
        long id = Math.incrementExact(existentes.stream().mapToLong(Pelicula::id).max().orElse(0));
        Pelicula pelicula = new Pelicula(id, titulo, duracion, clasificacion, true);
        existentes.add(pelicula);
        guardar("peliculas.csv", PELICULAS, existentes.stream()
                .map(p -> ArchivoCsv.fila(p.id(), p.titulo(), p.duracion(), p.clasificacion(), p.activa()))
                .toList());
        return pelicula;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Set<String> ocupados(long funcionId) {
        return ocupados(compras(), funcionId);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Optional<Compra> buscar(long folio) {
        return compras().stream().filter(c -> c.folio() == folio).findFirst();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized long registrar(Funcion funcion, List<String> asientos, Clock reloj) {
        List<Compra> existentes = compras();
        LocalDateTime fecha = LocalDateTime.now(reloj);
        if (funcion.inicio().isBefore(fecha)) {
            throw new IllegalStateException("La función ya comenzó. Elige otra función.");
        }
        Set<String> ocupados = ocupados(existentes, funcion.id());
        for (String asiento : asientos) {
            if (ocupados.contains(asiento)) {
                throw new IllegalArgumentException("El asiento " + asiento + " ya está vendido. Elige otros lugares.");
            }
        }
        long folio = Math.incrementExact(existentes.stream().mapToLong(Compra::folio).max().orElse(0));
        existentes.add(new Compra(folio, funcion.id(), fecha, asientos, funcion.precio(), true));
        guardar("compras.csv", COMPRAS, existentes.stream().map(c -> ArchivoCsv.fila(
                c.folio(), c.funcionId(), c.fecha(), String.join(";", c.asientos()), c.precio(), c.vigente()))
                .toList());
        return folio;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Optional<Trabajador> buscarTrabajador(String usuario) {
        return leer("trabajadores.csv", TRABAJADORES,
                r -> new Trabajador(r.get(0), r.get(1), r.get(2))).stream()
                .filter(t -> t.usuario().equals(usuario)).findFirst();
    }

    /**
     * Libera el directorio para otra instancia.
     * 
     * @throws IOException si no se puede cerrar el archivo de bloqueo
     */
    @Override
    public synchronized void close() throws IOException {
        try {
            bloqueo.release();
        } finally {
            canal.close();
        }
    }

    private List<Pelicula> peliculas() {
        return leer("peliculas.csv", PELICULAS, r -> new Pelicula(largo(r, 0), r.get(1),
                entero(r, 2), r.get(3), booleano(r, 4)));
    }

    private List<Compra> compras() {
        return leer("compras.csv", COMPRAS, r -> new Compra(largo(r, 0), largo(r, 1),
                LocalDateTime.parse(r.get(2)), Arrays.asList(r.get(3).split(";", -1)),
                new BigDecimal(r.get(4)), booleano(r, 5)));
    }

    private static Set<String> ocupados(List<Compra> compras, long funcionId) {
        Set<String> asientos = new HashSet<>();
        compras.stream().filter(c -> c.funcionId() == funcionId && c.vigente())
                .forEach(c -> asientos.addAll(c.asientos()));
        return Set.copyOf(asientos);
    }

    private <T> List<T> leer(String archivo, String cabecera, Function<List<String>, T> conversion) {
        if (!bloqueo.isValid()) {
            throw new IllegalStateException("El almacén CSV está cerrado.");
        }
        try {
            return ArchivoCsv.leer(carpeta.resolve(archivo), cabecera).stream()
                    .map(conversion).collect(Collectors.toCollection(java.util.ArrayList::new));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo + ". Revisa los datos locales.", e);
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            throw new IllegalStateException("Hay datos inválidos en " + archivo + ". Revisa el CSV.", e);
        }
    }

    private void guardar(String archivo, String cabecera, List<String> filas) {
        try {
            ArchivoCsv.guardar(carpeta.resolve(archivo), cabecera, filas);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo + ". Revisa el directorio y reintenta.", e);
        }
    }

    private static <K, V> V referencia(Map<K, V> mapa, K clave) {
        V valor = mapa.get(clave);
        if (valor == null) {
            throw new IllegalArgumentException("Referencia de catálogo inexistente.");
        }
        return valor;
    }

    private static long largo(List<String> fila, int columna) {
        return Long.parseLong(fila.get(columna));
    }

    private static int entero(List<String> fila, int columna) {
        return Integer.parseInt(fila.get(columna));
    }

    private static boolean booleano(List<String> fila, int columna) {
        return switch (fila.get(columna)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Estado CSV inválido.");
        };
    }
}
