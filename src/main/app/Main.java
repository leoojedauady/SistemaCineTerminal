package app;

import consola.MenuCine;
import modelo.Trabajador;
import persistencia.AlmacenCsv;
import seguridad.Clave;
import servicio.PersonalServicio;
import servicio.VentaServicio;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Scanner;

/** Punto de entrada y ensamblado de los servicios con persistencia CSV. */
public final class Main {
    private Main() {}

    /**
     * Inicia el cine con datos de demostración nuevos para esta sesión.
     * @param args argumentos de terminal; la aplicación se ejecuta sin argumentos
     */
    public static void main(String[] args) {
        int estado = iniciar();
        if (estado != 0) {
            System.exit(estado);
        }
    }

    /** Prepara los CSV temporales, conecta los servicios y muestra el menú. */
    private static int iniciar() {
        PrintWriter salida = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        PrintWriter errores = new PrintWriter(System.err, true, StandardCharsets.UTF_8);
        Path carpeta = null;
        try {
            carpeta = Files.createTempDirectory("cine-equipo5-");
            Clock reloj = Clock.systemDefaultZone();
            cargarDatos(carpeta, reloj);
            salida.println("Cuenta de demostración: taquilla / CineDemo2026");
            try (AlmacenCsv almacen = new AlmacenCsv(carpeta);
                 Scanner entrada = new Scanner(System.in, StandardCharsets.UTF_8)) {
                VentaServicio ventas = new VentaServicio(almacen, almacen, reloj);
                PersonalServicio personal = new PersonalServicio(almacen, almacen, ventas);
                new MenuCine(entrada, salida, System.console(), ventas, personal).ejecutar();
            }
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            errores.println("No se pudo abrir el cine. " + e.getMessage());
            return 1;
        } finally {
            if (carpeta != null) {
                limpiarDatos(carpeta, errores);
            }
        }
    }

    /** Crea una función de hoy y guarda únicamente la sal y el hash de la cuenta. */
    private static void cargarDatos(Path carpeta, Clock reloj) throws IOException {
        char[] inicial = "CineDemo2026".toCharArray();
        try {
            Trabajador trabajador = Clave.crear("taquilla", inicial);
            LocalDateTime ahora = LocalDateTime.now(reloj);
            LocalDateTime inicio = ahora.toLocalDate().atTime(23, 59, 59, 999_999_999);
            AlmacenCsv.prepararDemo(carpeta, inicio, trabajador);
        } finally {
            Arrays.fill(inicial, '\0');
        }
    }

    /** Retira los archivos de esta sesión una vez liberado el bloqueo del almacén. */
    private static void limpiarDatos(Path carpeta, PrintWriter errores) {
        try {
            try (var archivos = Files.newDirectoryStream(carpeta)) {
                for (Path archivo : archivos) {
                    Files.deleteIfExists(archivo);
                }
            }
            Files.deleteIfExists(carpeta);
        } catch (IOException e) {
            errores.println("No se pudieron retirar todos los datos temporales. Revisa: " + carpeta);
        }
    }
}
