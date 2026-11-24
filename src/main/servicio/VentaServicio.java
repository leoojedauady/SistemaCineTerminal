package servicio;

import modelo.Boleto;
import modelo.Compra;
import modelo.Disponibilidad;
import modelo.Funcion;
import modelo.Seleccion;
import persistencia.RepositorioCatalogo;
import persistencia.RepositorioCompras;
import validacion.SeleccionAsientos;

import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Cartelera y compra de boletos para clientes anónimos. */
public final class VentaServicio {
    private final RepositorioCatalogo catalogo;
    private final RepositorioCompras compras;
    private final Clock reloj;

    /**
     * Configura los puertos y el reloj local.
     * @param catalogo programación de funciones
     * @param compras almacenamiento de ventas
     * @param reloj fuente de fecha y hora
     */
    public VentaServicio(RepositorioCatalogo catalogo, RepositorioCompras compras, Clock reloj) {
        this.catalogo = catalogo;
        this.compras = compras;
        this.reloj = reloj;
    }

    /**
     * Obtiene la cartelera de hoy por orden de inicio.
     * @return funciones del día con su disponibilidad actual
     */
    public List<Disponibilidad> cartelera() {
        LocalDate hoy = LocalDate.now(reloj);
        return catalogo.funciones().stream()
                .filter(f -> f.inicio().toLocalDate().equals(hoy))
                .sorted(Comparator.comparing(Funcion::inicio).thenComparingLong(Funcion::id))
                .map(this::disponibilidad)
                .toList();
    }

    /**
     * Consulta nuevamente la ocupación de una función de hoy.
     * @param funcionId identificador de la función
     * @return proyección y lugares ocupados
     */
    public Disponibilidad mapa(long funcionId) {
        return disponibilidad(funcionDeHoy(funcionId));
    }

    /**
     * Valida la selección actual y prepara el resumen.
     * @param funcionId identificador de la función del día
     * @param texto selección separada por comas
     * @return selección e importe calculable
     */
    public Seleccion seleccionar(long funcionId, String texto) {
        Disponibilidad disponibilidad = mapa(funcionId);
        List<String> asientos = SeleccionAsientos.interpretar(texto, disponibilidad.funcion().sala());
        Set<String> ocupados = disponibilidad.ocupados();
        for (String asiento : asientos) {
            if (ocupados.contains(asiento)) {
                throw new IllegalArgumentException("El asiento " + asiento + " está ocupado. Elige otro lugar.");
            }
        }
        return new Seleccion(disponibilidad, asientos);
    }

    /**
     * Reconstruye los datos del boleto a partir de los registros.
     * Visible dentro del paquete para la consulta autenticada.
     * @param folio folio registrado
     * @return compra y función originales
     */
    Boleto leerBoleto(long folio) {
        Compra compra = compras.buscar(folio).orElseThrow(() ->
                new IllegalArgumentException("No existe una compra con el folio indicado"));
        return new Boleto(funcion(compra.funcionId()), compra);
    }

    /**
     * Obtiene el boleto como resultado de la compra del cliente.
     * El comprobante se vincula a una acción de confirmación, sin ofrecer búsqueda pública.
     * @param funcionId identificador de la función
     * @param texto selección confirmada
     * @return boleto reconstruido desde el almacenamiento
     */
    public Boleto confirmar(long funcionId, String texto) {
        Funcion funcion = funcionDeHoy(funcionId);
        List<String> asientos = SeleccionAsientos.interpretar(texto, funcion.sala());
        long folio = compras.registrar(funcion, asientos, reloj);
        try {
            return leerBoleto(folio);
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException e) {
            throw new IllegalStateException("Compra registrada con folio " + folio
                    + ". El boleto no se pudo leer. Consulta el folio en taquilla.", e);
        }
    }

    private Funcion funcionDeHoy(long id) {
        Funcion funcion = funcion(id);
        if (!funcion.inicio().toLocalDate().equals(LocalDate.now(reloj))) {
            throw new IllegalArgumentException("La función no pertenece a la cartelera de hoy. Elige otra.");
        }
        return funcion;
    }

    private Funcion funcion(long id) {
        return catalogo.funciones().stream().filter(f -> f.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La función no existe. Elige otra."));
    }

    private Disponibilidad disponibilidad(Funcion funcion) {
        return new Disponibilidad(funcion, compras.ocupados(funcion.id()));
    }
}
