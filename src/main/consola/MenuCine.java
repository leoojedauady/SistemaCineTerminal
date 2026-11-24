package consola;

import modelo.Boleto;
import modelo.Compra;
import modelo.Disponibilidad;
import modelo.Funcion;
import modelo.Pelicula;
import modelo.Sala;
import modelo.Seleccion;
import servicio.PersonalServicio;
import servicio.VentaServicio;

import java.io.Console;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/** Menús en español. Las consultas y escrituras se realizan mediante los servicios. */
public final class MenuCine {
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private final Scanner entrada;
    private final PrintWriter salida;
    private final Console terminal;
    private final VentaServicio ventas;
    private final PersonalServicio personal;

    /**
     * Conecta la consola con los servicios.
     * @param entrada lector para las entradas de menú
     * @param salida salida de texto
     * @param terminal consola que permite ocultar la contraseña, si está disponible
     * @param ventas servicio de clientes
     * @param personal servicio de trabajadores
     */
    public MenuCine(Scanner entrada, PrintWriter salida, Console terminal,
                    VentaServicio ventas, PersonalServicio personal) {
        this.entrada = entrada;
        this.salida = salida;
        this.terminal = terminal;
        this.ventas = ventas;
        this.personal = personal;
    }

    /** Ejecuta el menú principal hasta que se elige salir o finaliza la entrada. */
    public void ejecutar() {
        while (true) {
            separar("CINE | EQUIPO 5");
            salida.println("1. Soy cliente");
            salida.println("2. Soy trabajador");
            salida.println("0. Salir");
            String opcion = leer("Opción: ");
            if (opcion == null || opcion.equals("0")) {
                return;
            }
            intentar(() -> {
                switch (opcion) {
                    case "1" -> cliente();
                    case "2" -> trabajador();
                    default -> salida.println("Opción inválida. Elige 0, 1 o 2.");
                }
            });
        }
    }

    private void cliente() {
        while (true) {
            separar("MENÚ DE CLIENTE");
            salida.println("1. Ver cartelera y comprar");
            salida.println("0. Regresar");
            String opcion = leer("Opción: ");
            if (opcion == null || opcion.equals("0")) {
                return;
            }
            if (opcion.equals("1")) {
                intentar(this::cartelera);
            } else {
                salida.println("Opción inválida. Elige 0 o 1.");
            }
        }
    }

    private void cartelera() {
        separar("CARTELERA DEL DÍA");
        List<Disponibilidad> cartelera = ventas.cartelera();
        if (cartelera.isEmpty()) {
            salida.println("No hay funciones programadas para hoy");
            return;
        }

        for (int indice = 0; indice < cartelera.size(); indice++) {
            Disponibilidad vista = cartelera.get(indice);
            Funcion funcion = vista.funcion();
            Pelicula pelicula = funcion.pelicula();
            salida.printf("%d. %s%n", indice + 1, pelicula.titulo());
            salida.printf("   %d min | Clas. %s | Sala %d | %s | %d disponibles%n",
                    pelicula.duracion(), pelicula.clasificacion(), funcion.sala().numero(),
                    HORA.format(funcion.inicio()), vista.libres());
        }
        while (true) {
            String opcion = leer("Función (0 para regresar): ");
            if (opcion == null || opcion.equals("0")) {
                return;
            }
            int numero;
            try {
                numero = Integer.parseInt(opcion);
            } catch (NumberFormatException e) {
                salida.println("Escribe un número de la cartelera.");
                continue;
            }
            if (numero < 1 || numero > cartelera.size()) {
                salida.println("La función no existe. Elige un número de la cartelera.");
                continue;
            }
            flujoCompra(cartelera.get(numero - 1).funcion().id());
            return;
        }
    }

    /** Muestra el mapa, valida la selección y solicita permiso antes de registrar la compra. */
    private void flujoCompra(long funcionId) {
        while (true) {
            mapa(ventas.mapa(funcionId), List.of());
            String asientos = leer("Asientos separados por comas (0 para regresar): ");
            if (asientos == null || asientos.equals("0")) {
                return;
            }

            Seleccion seleccion;
            try {
                seleccion = ventas.seleccionar(funcionId, asientos);
            } catch (IllegalArgumentException e) {
                salida.println(e.getMessage());
                continue;
            }
            mapa(seleccion.disponibilidad(), seleccion.asientos());
            Funcion funcion = seleccion.disponibilidad().funcion();
            separar("RESUMEN DE COMPRA");
            datosFuncion(funcion);
            salida.println("Asientos: " + String.join(", ", seleccion.asientos()));
            salida.println("Precio por asiento: " + pesos(funcion.precio()));
            salida.println("Importe total: " + pesos(seleccion.total()));
            if (!confirmacion()) {
                salida.println("No se registró ninguna compra.");
                return;
            }
            try {
                imprimirBoleto(ventas.confirmar(funcionId, asientos), false);
                return;
            } catch (IllegalArgumentException e) {
                salida.println(e.getMessage());
            }
        }
    }

    /** Abre el menú autenticado y cierra la sesión incluso si termina la entrada. */
    private void trabajador() {
        String usuario = leer("Usuario (0 para regresar): ");
        if (usuario == null || usuario.equals("0")) {
            return;
        }
        char[] contrasena;
        if (terminal == null) {
            salida.print("Contraseña: ");
            salida.flush();
            if (!entrada.hasNextLine()) {
                return;
            }
            contrasena = entrada.nextLine().toCharArray();
        } else {
            contrasena = terminal.readPassword("Contraseña: ");
        }
        if (contrasena == null) {
            return;
        }
        personal.entrar(usuario, contrasena);
        try {
            menuTrabajador();
        } finally {
            personal.salir();
        }
    }

    private void menuTrabajador() {
        while (true) {
            separar("MENÚ DE TRABAJADOR");
            salida.println("1. Consultar compra por folio");
            salida.println("2. Registrar película");
            salida.println("0. Cerrar sesión");
            String opcion = leer("Opción: ");
            if (opcion == null || opcion.equals("0")) {
                return;
            }
            intentar(() -> {
                switch (opcion) {
                    case "1" -> consultar();
                    case "2" -> registrarPelicula();
                    default -> salida.println("Opción inválida. Elige 0, 1 o 2.");
                }
            });
        }
    }

    private void consultar() {
        String folio = leer("Folio (0 para regresar): ");
        if (folio != null && !folio.equals("0")) {
            imprimirBoleto(personal.consultar(Long.parseLong(folio)), true);
        }
    }

    private void registrarPelicula() {
        String titulo = leer("Título (0 para regresar): ");
        if (titulo == null || titulo.equals("0")) {
            return;
        }
        String duracion = leer("Duración en minutos: ");
        if (duracion == null) {
            return;
        }
        int minutos = Integer.parseInt(duracion);
        String clasificacion = leer("Clasificación: ");
        if (clasificacion == null) {
            return;
        }
        Pelicula pelicula = personal.registrarPelicula(titulo, minutos, clasificacion);
        salida.println("Película activa registrada con ID " + pelicula.id() + ".");
    }

    private void mapa(Disponibilidad vista, List<String> seleccion) {
        Funcion funcion = vista.funcion();
        Sala sala = funcion.sala();
        separar("MAPA DE ASIENTOS");
        datosFuncion(funcion);
        salida.println(pesos(funcion.precio()) + " por asiento");
        salida.println("PANTALLA");
        salida.print("  ");
        for (int columna = 1; columna <= sala.columnas(); columna++) {
            salida.printf("%2d ", columna);
        }
        salida.println();
        for (char fila = 'A'; fila < 'A' + sala.filas(); fila++) {
            salida.print(fila + " ");
            for (int columna = 1; columna <= sala.columnas(); columna++) {
                String asiento = String.valueOf(fila) + columna;
                String marca = vista.ocupados().contains(asiento) ? "[X]"
                        : seleccion.contains(asiento) ? "[*]" : "[ ]";
                salida.print(marca);
            }
            salida.println();
        }
        salida.println("[ ] Libre  [X] Ocupado  [*] Seleccionado");
    }

    private void imprimirBoleto(Boleto boleto, boolean historico) {
        Compra compra = boleto.compra();
        separar(historico ? "CONSULTA DE COMPRA" : "BOLETO");
        salida.println("Folio: " + compra.folio());
        if (historico) {
            salida.println("Fecha de compra: " + FECHA_HORA.format(compra.fecha()));
            salida.println("Estado: " + (compra.vigente() ? "Vigente" : "Cancelada"));
        }
        datosFuncion(boleto.funcion());
        for (String asiento : compra.asientos()) {
            salida.printf("Asiento %-4s ..................... %s%n", asiento, pesos(compra.precio()));
        }
        salida.println("IMPORTE TOTAL: " + pesos(compra.total()));
    }

    private void datosFuncion(Funcion funcion) {
        salida.println("Película: " + funcion.pelicula().titulo());
        salida.println("Sala: " + funcion.sala().numero());
        salida.println("Fecha y hora: " + FECHA_HORA.format(funcion.inicio()));
    }

    private boolean confirmacion() {
        while (true) {
            String respuesta = leer("¿Confirmar compra? (S/N): ");
            if (respuesta == null || respuesta.equalsIgnoreCase("N")) {
                return false;
            }
            if (respuesta.equalsIgnoreCase("S")) {
                return true;
            }
            salida.println("Escribe S para confirmar o N para regresar.");
        }
    }

    private void intentar(Runnable accion) {
        try {
            accion.run();
        } catch (NumberFormatException e) {
            salida.println("Número inválido. Escribe un entero dentro del rango.");
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException e) {
            salida.println(e.getMessage());
        }
    }

    private String leer(String mensaje) {
        salida.print(mensaje);
        salida.flush();
        if (terminal != null) {
            String linea = terminal.readLine();
            return linea == null ? null : linea.strip();
        }
        return entrada.hasNextLine() ? entrada.nextLine().strip() : null;
    }

    private void separar(String titulo) {
        salida.println();
        salida.println("=== " + titulo + " ===");
    }

    private static String pesos(BigDecimal importe) {
        return "$" + importe.toPlainString();
    }
}
