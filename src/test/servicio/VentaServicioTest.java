package servicio;

import modelo.Boleto;
import modelo.Trabajador;
import persistencia.AlmacenCsv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VentaServicioTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-22T16:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime INICIO = LocalDateTime.now(RELOJ).plusHours(1);

    @TempDir
    Path carpeta;

    @BeforeEach
    void preparar() throws IOException {
        AlmacenCsv.prepararDemo(carpeta, INICIO, new Trabajador("pruebas", "AA==", "AA=="));
    }

    @Test
    void compraMultiple_persisteBoletoYActualizaCarteleraAlReabrir() throws IOException {
        long folio;
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            VentaServicio ventas = new VentaServicio(almacen, almacen, RELOJ);
            assertEquals(32, ventas.cartelera().getFirst().libres());
            assertEquals(new BigDecimal("140.00"), ventas.seleccionar(1, "A1,B2").total());
            Boleto boleto = ventas.confirmar(1, "A1,B2");
            assertEquals(List.of("A1", "B2"), boleto.compra().asientos());
            assertEquals(new BigDecimal("140.00"), boleto.compra().total());
            folio = boleto.compra().folio();
        }
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            VentaServicio ventas = new VentaServicio(almacen, almacen, RELOJ);
            assertEquals(Set.of("A1", "B2"), ventas.mapa(1).ocupados());
            assertEquals(30, ventas.cartelera().getFirst().libres());
            assertEquals(folio, ventas.leerBoleto(folio).compra().folio());
            assertTrue(ventas.confirmar(1, "A2").compra().folio() > folio);
        }
    }

    @Test
    void justoEnHoraDeInicio_aceptaUnAsiento() throws IOException {
        Clock justo = Clock.fixed(INICIO.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            VentaServicio ventas = new VentaServicio(almacen, almacen, justo);
            assertEquals(List.of("D8"), ventas.confirmar(1, "D8").compra().asientos());
        }
    }

    @Test
    void seleccionDesactualizada_rechazaTodoElGrupo() throws IOException {
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            VentaServicio ventas = new VentaServicio(almacen, almacen, RELOJ);
            ventas.seleccionar(1, "A1,A2");
            Boleto primera = ventas.confirmar(1, "A2");
            assertThrows(IllegalArgumentException.class, () -> ventas.confirmar(1, "A1,A2"));
            assertEquals(Set.of("A2"), ventas.mapa(1).ocupados());
            assertTrue(almacen.buscar(primera.compra().folio() + 1).isEmpty());
        }
    }

    @Test
    void funcionEmpezada_rechazaSinOcuparAsientos() throws IOException {
        Clock tarde = Clock.fixed(INICIO.plusNanos(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            VentaServicio ventas = new VentaServicio(almacen, almacen, tarde);
            assertThrows(IllegalStateException.class, () -> ventas.confirmar(1, "A1"));
            assertTrue(ventas.mapa(1).ocupados().isEmpty());
        }
    }

    @Test
    void funcionInexistente_seRechazaYCarteleraDeOtroDiaEstaVacia() throws IOException {
        Clock manana = Clock.offset(RELOJ, java.time.Duration.ofDays(1));
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            VentaServicio ventas = new VentaServicio(almacen, almacen, manana);
            assertTrue(ventas.cartelera().isEmpty());
            assertThrows(IllegalArgumentException.class, () -> ventas.mapa(99));
            assertThrows(IllegalArgumentException.class, () -> ventas.confirmar(1, "A1"));
        }
    }

    @Test
    void dosHilosEnElMismoAlmacen_soloUnaCompraSeRegistra() throws Exception {
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta);
             var executor = Executors.newFixedThreadPool(2)) {
            VentaServicio ventas = new VentaServicio(almacen, almacen, RELOJ);
            Callable<Boolean> compra = () -> {
                try {
                    ventas.confirmar(1, "A1");
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            };
            var resultados = executor.invokeAll(List.of(compra, compra), 5, TimeUnit.SECONDS);
            assertNotEquals(resultados.get(0).get(), resultados.get(1).get());
            assertEquals(Set.of("A1"), ventas.mapa(1).ocupados());
        }
    }

    @Test
    void mismoDirectorio_rechazaUnSegundoAlmacen() throws IOException {
        try (AlmacenCsv primero = new AlmacenCsv(carpeta)) {
            IOException error = assertThrows(IOException.class, () -> new AlmacenCsv(carpeta));
            assertTrue(error.getMessage().contains("otra terminal"));
        }
    }
}
