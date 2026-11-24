package servicio;

import modelo.Boleto;
import modelo.Pelicula;
import modelo.Trabajador;
import persistencia.RepositorioCatalogo;
import persistencia.RepositorioTrabajadores;
import seguridad.Clave;

import java.util.Arrays;

/** Sesión de trabajador y operaciones autorizadas del avance. */
public final class PersonalServicio {
    private static final String CREDENCIAL_INVALIDA = "Usuario o contraseña incorrectos";
    private final RepositorioTrabajadores trabajadores;
    private final RepositorioCatalogo catalogo;
    private final VentaServicio ventas;
    private final Trabajador credencialFicticia;
    private String usuarioActivo;

    /**
     * Crea un servicio para una sesión de consola.
     * @param trabajadores fuente de cuentas
     * @param catalogo almacenamiento de películas
     * @param ventas fuente de datos del boleto
     */
    public PersonalServicio(RepositorioTrabajadores trabajadores, RepositorioCatalogo catalogo, VentaServicio ventas) {
        this.trabajadores = trabajadores;
        this.catalogo = catalogo;
        this.ventas = ventas;
        credencialFicticia = Clave.crear("", new char[]{'x'});
    }

    /**
     * Valida las credenciales y abre la sesión. Limpia el arreglo de contraseña recibido.
     * @param usuario nombre de acceso
     * @param contrasena contraseña temporal
     */
    public void entrar(String usuario, char[] contrasena) {
        usuarioActivo = null;
        try {
            if (usuario.isBlank() || contrasena.length == 0) {
                throw new IllegalArgumentException(CREDENCIAL_INVALIDA);
            }
            Trabajador trabajador = trabajadores.buscarTrabajador(usuario).orElse(credencialFicticia);
            if (!Clave.verificar(contrasena, trabajador) || trabajador == credencialFicticia) {
                throw new IllegalArgumentException(CREDENCIAL_INVALIDA);
            }
            usuarioActivo = trabajador.usuario();
        } finally {
            Arrays.fill(contrasena, '\0');
        }
    }

    /** Cierra la sesión actual. */
    public void salir() {
        usuarioActivo = null;
    }

    /**
     * Consulta una compra histórica con autorización.
     * @param folio folio numérico
     * @return datos de la compra y de la función
     */
    public Boleto consultar(long folio) {
        exigirSesion();
        return ventas.leerBoleto(folio);
    }

    /**
     * Valida y registra una película activa con autorización.
     * @param titulo título
     * @param duracion minutos de 1 a 600
     * @param clasificacion clasificación de audiencia
     * @return película creada
     */
    public Pelicula registrarPelicula(String titulo, int duracion, String clasificacion) {
        exigirSesion();
        if (duracion < 1 || duracion > 600) {
            throw new IllegalArgumentException("La duración debe ser de 1 a 600 minutos.");
        }
        if (!textoValido(titulo) || !textoValido(clasificacion)) {
            throw new IllegalArgumentException("Escribe título y clasificación, de 1 a 100 caracteres por campo.");
        }
        return catalogo.agregarPelicula(titulo.strip(), duracion, clasificacion.strip());
    }

    private void exigirSesion() {
        if (usuarioActivo == null) {
            throw new IllegalStateException("Inicia sesión como trabajador para continuar.");
        }
    }

    private static boolean textoValido(String texto) {
        return !texto.isBlank() && texto.strip().length() <= 100
                && texto.codePoints().noneMatch(Character::isISOControl);
    }
}
