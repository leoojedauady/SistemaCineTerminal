package validacion;

import modelo.Sala;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeleccionAsientosTest {
    private final Sala sala = new Sala(1, 4, 8);

    @Test
    void seleccionMultiple_normalizaYConservaElOrden() {
        assertEquals(List.of("B3", "A1"), SeleccionAsientos.interpretar(" b3, a1 ", sala));
    }

    @Test
    void unicoAsientoEnExtremo_deLaSalaSeAcepta() {
        assertEquals(List.of("D8"), SeleccionAsientos.interpretar("D8", sala));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A1,", "E1", "A9", "A0", "A1, a1", "A01"})
    void seleccionInvalida_seRechaza(String texto) {
        assertThrows(IllegalArgumentException.class, () -> SeleccionAsientos.interpretar(texto, sala));
    }
}
