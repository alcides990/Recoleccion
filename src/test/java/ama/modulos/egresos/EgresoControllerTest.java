package ama.modulos.egresos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EgresoControllerTest {
    private final EgresoService gastos = mock(EgresoService.class);
    private final CatalogoService catalogos = mock(CatalogoService.class);
    private MockMvc mvc;

    @BeforeEach void iniciar() {
        var sesion = mock(EgresoSesion.class);
        when(sesion.actual()).thenReturn(new EgresoSesion.Contexto(7, 9, "Sucursal"));
        mvc = MockMvcBuilders.standaloneSetup(new EgresoController(gastos, catalogos, sesion))
                .setControllerAdvice(new EgresoErrores()).build();
    }

    @Test void conservaContratoJsonDeDetalle() throws Exception {
        when(gastos.detalle(7, 1)).thenReturn(new EgresoDto(1, LocalDate.of(2026,9,8), 3,
                "Tipo", "Proveedor", "001", "", BigDecimal.TEN, false,
                List.of(new LineaEgreso("Aceite", BigDecimal.ONE, BigDecimal.TEN))));
        mvc.perform(get("/egresos/1/datos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-09-08"))
                .andExpect(jsonPath("$.tipo_id").value(3))
                .andExpect(jsonPath("$.anulado").value(false))
                .andExpect(jsonPath("$.detalles[0].producto").value("Aceite"));
    }

    @Test void traduceErrorDeNegocioSinExponerSql() throws Exception {
        doThrow(EgresoException.datosInvalidos("Gasto no encontrado o ya anulado."))
                .when(gastos).anular(7, 99);
        mvc.perform(post("/egresos/99/anular")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Gasto no encontrado o ya anulado."));
    }

    @Test void rechazaClaseDeCatalogoInvalida() throws Exception {
        mvc.perform(get("/egresos/catalogos/OTRO")).andExpect(status().isBadRequest());
        verifyNoInteractions(catalogos);
    }
}
