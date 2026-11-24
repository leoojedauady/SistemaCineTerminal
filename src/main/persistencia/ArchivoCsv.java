package persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lee y reemplaza archivos CSV UTF-8 con campos entrecomillados en una sola línea. */
final class ArchivoCsv {
    private static final Pattern CAMPO = Pattern.compile("\"((?:[^\"\\r\\n]|\"\")*)\"(,|$)");

    private ArchivoCsv() {}

    static List<List<String>> leer(Path ruta, String cabecera) throws IOException {
        List<String> lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        if (lineas.isEmpty() || !lineas.getFirst().equals(cabecera)) {
            throw new IOException("Cabecera CSV incorrecta: " + ruta.getFileName());
        }

        int columnas = cabecera.split(",").length;
        List<List<String>> registros = new ArrayList<>();
        for (String linea : lineas.subList(1, lineas.size())) {
            Matcher matcher = CAMPO.matcher(linea);
            List<String> fila = new ArrayList<>();
            int posicion = 0;
            while (posicion < linea.length() && matcher.find(posicion) && matcher.start() == posicion) {
                fila.add(matcher.group(1).replace("\"\"", "\""));
                posicion = matcher.end();
            }
            if (posicion != linea.length() || fila.size() != columnas || linea.endsWith(",")) {
                throw new IOException("Registro CSV incorrecto: " + ruta.getFileName());
            }
            registros.add(fila);
        }
        return registros;
    }

    static String fila(Object... valores) {
        return String.join(",", Arrays.stream(valores).map(Object::toString)
                .map(valor -> "\"" + valor.replace("\"", "\"\"") + "\"").toList());
    }

    /** Completa la escritura junto al archivo original antes de sustituirlo de forma atómica. */
    static void guardar(Path ruta, String cabecera, List<String> filas) throws IOException {
        Path temporal = Files.createTempFile(ruta.getParent(), "cine-", ".tmp");
        try {
            List<String> lineas = new ArrayList<>();
            lineas.add(cabecera);
            lineas.addAll(filas);
            Files.write(temporal, lineas, StandardCharsets.UTF_8);
            Files.move(temporal, ruta, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }
}
