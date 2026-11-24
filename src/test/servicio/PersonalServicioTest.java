package servicio;

import modelo.Pelicula;
import modelo.Trabajador;
import persistencia.AlmacenCsv;
import seguridad.Clave;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PersonalServicioTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-22T16:00:00Z"), ZoneOffset.UTC);
    private static final String USUARIO = "taquilla";
    private static final String CONTRASENA = "ClaveTemporalDePrueba";

    @TempDir
    Path carpeta;

    @BeforeEach
    void preparar() throws IOException {
        AlmacenCsv.prepararDemo(carpeta, LocalDateTime.now(RELOJ).plusHours(1),
                Clave.crear(USUARIO, CONTRASENA.toCharArray()));
    }

    @Test
    void sesionValida_permiteConsultarYRegistrarPelicula() throws IOException {
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            VentaServicio ventas = new VentaServicio(almacen, almacen, RELOJ);
            var compra = ventas.confirmar(1, "D4").compra();
            PersonalServicio personal = new PersonalServicio(almacen, almacen, ventas);
            char[] clave = CONTRASENA.toCharArray();
            personal.entrar(USUARIO, clave);
            assertArrayEquals(new char[clave.length], clave);
            assertEquals(compra, personal.consultar(compra.folio()).compra());
            Pelicula pelicula = personal.registrarPelicula("La \"Luna\", otra vez", 120, "B");
            assertEquals(2, pelicula.id());
            assertTrue(pelicula.activa());
        }
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            PersonalServicio personal = personal(almacen);
            personal.entrar(USUARIO, CONTRASENA.toCharArray());
            assertEquals(3, personal.registrarPelicula("Siguiente", 90, "A").id());
        }
        assertTrue(Files.readString(carpeta.resolve("peliculas.csv")).contains("\"\"Luna\"\", otra vez"));
    }

    @Test
    void duracionesMinimaYMaxima_seAceptan() throws IOException {
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            PersonalServicio personal = personal(almacen);
            personal.entrar(USUARIO, CONTRASENA.toCharArray());
            assertEquals(1, personal.registrarPelicula("Corto", 1, "A").duracion());
            assertEquals(600, personal.registrarPelicula("Maratón", 600, "A").duracion());
        }
    }

    @Test
    void datosDePeliculaInvalidos_noSeGuardan() throws IOException {
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            PersonalServicio personal = personal(almacen);
            personal.entrar(USUARIO, CONTRASENA.toCharArray());
            String anterior = Files.readString(carpeta.resolve("peliculas.csv"));
            assertThrows(IllegalArgumentException.class, () -> personal.registrarPelicula("A", 0, "B"));
            assertThrows(IllegalArgumentException.class, () -> personal.registrarPelicula("A", 601, "B"));
            assertThrows(IllegalArgumentException.class, () -> personal.registrarPelicula("", 90, "B"));
            assertThrows(IllegalArgumentException.class, () -> personal.registrarPelicula("A", 90, " "));
            assertEquals(anterior, Files.readString(carpeta.resolve("peliculas.csv")));
        }
    }

    @Test
    void credencialIncorrecta_mensajeUniformeYSinPermisos() throws IOException {
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            PersonalServicio personal = personal(almacen);
            var clave = assertThrows(IllegalArgumentException.class,
                    () -> personal.entrar(USUARIO, "mal".toCharArray()));
            var usuario = assertThrows(IllegalArgumentException.class,
                    () -> personal.entrar("otro", CONTRASENA.toCharArray()));
            assertEquals("Usuario o contraseña incorrectos", usuario.getMessage());
            assertEquals(clave.getMessage(), usuario.getMessage());
            assertThrows(IllegalStateException.class, () -> personal.consultar(1));
        }
    }

    @Test
    void cerrarSesion_revocaPermisosYFolioInexistenteSeRechaza() throws IOException {
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            PersonalServicio personal = personal(almacen);
            personal.entrar(USUARIO, CONTRASENA.toCharArray());
            assertThrows(IllegalArgumentException.class, () -> personal.consultar(999));
            personal.salir();
            assertThrows(IllegalStateException.class, () -> personal.registrarPelicula("Título", 90, "B"));
            assertThrows(IllegalStateException.class, () -> personal.consultar(999));
        }
    }

    @Test
    void mismaContrasena_obtieneSalesYHashesDistintos() throws IOException {
        try (AlmacenCsv almacen = new AlmacenCsv(carpeta)) {
            Trabajador persistido = almacen.buscarTrabajador(USUARIO).orElseThrow();
            Trabajador otro = Clave.crear("otro", CONTRASENA.toCharArray());
            assertTrue(Base64.getDecoder().decode(persistido.sal()).length >= 16);
            assertNotEquals(persistido.sal(), otro.sal());
            assertNotEquals(persistido.hash(), otro.hash());
        }
        assertFalse(Files.readString(carpeta.resolve("trabajadores.csv")).contains(CONTRASENA));
    }

    private static PersonalServicio personal(AlmacenCsv almacen) {
        return new PersonalServicio(almacen, almacen, new VentaServicio(almacen, almacen, RELOJ));
    }
}
