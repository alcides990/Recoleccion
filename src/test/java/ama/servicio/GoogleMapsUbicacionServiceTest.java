package ama.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GoogleMapsUbicacionServiceTest {

    private final GoogleMapsUbicacionService servicio = new GoogleMapsUbicacionService();

    @Test
    void extraeCoordenadasPegadas() {
        var resultado = servicio.extraer("-25.2637000, -57.5759000");

        assertEquals(new BigDecimal("-25.2637000"), resultado.latitud());
        assertEquals(new BigDecimal("-57.5759000"), resultado.longitud());
    }

    @Test
    void extraeCoordenadasDeUrlOficialCodificada() {
        var resultado = servicio.extraer(
                "https://www.google.com/maps/search/?api=1&query=-25.2637%2C-57.5759");

        assertEquals(new BigDecimal("-25.2637"), resultado.latitud());
        assertEquals(new BigDecimal("-57.5759"), resultado.longitud());
    }

    @Test
    void extraeCoordenadasDeUrlCompartidaLarga() {
        var resultado = servicio.extraer(
                "https://www.google.com/maps/place/Asuncion/@-25.28646,-57.647,17z/data=!3m1!4b1");

        assertEquals(new BigDecimal("-25.28646"), resultado.latitud());
        assertEquals(new BigDecimal("-57.647"), resultado.longitud());
    }

    @Test
    void extraeCoordenadasEnGradosMinutosYSegundos() {
        var resultado = servicio.extraer("24°15'22.2\"S 54°45'09.5\"W");

        assertEquals(new BigDecimal("-24.2561666667"), resultado.latitud());
        assertEquals(new BigDecimal("-54.7526388889"), resultado.longitud());
    }

    @Test
    void extraeCoordenadasDmsConSimbolosTipograficosYOesteEnEspanol() {
        var resultado = servicio.extraer("24°15′22,2″S, 54°45′09,5″O");

        assertEquals(new BigDecimal("-24.2561666667"), resultado.latitud());
        assertEquals(new BigDecimal("-54.7526388889"), resultado.longitud());
    }

    @Test
    void rechazaUrlAjena() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.extraer("https://example.com/?q=-25.2,-57.5"));
    }
}
