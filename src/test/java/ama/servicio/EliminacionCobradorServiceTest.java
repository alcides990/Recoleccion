package ama.servicio;

import ama.dominio.Cobrador;
import ama.servicio.EliminacionCobradorService.CobradorConRegistrosRelacionadosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminacionCobradorServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private CobradorService cobradorService;
    @InjectMocks
    private EliminacionCobradorService servicio;

    @Test
    void noEliminaCuandoTieneRegistrosRelacionados() {
        Cobrador cobrador = new Cobrador(25);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(25)))
                .thenReturn(3, 1, 0, 0);

        var excepcion = assertThrows(CobradorConRegistrosRelacionadosException.class,
                () -> servicio.eliminar(cobrador));

        assertTrue(excepcion.getMessage().contains("comprobantes (3)"));
        assertTrue(excepcion.getMessage().contains("zonas (1)"));
        verify(cobradorService, never()).eliminar(cobrador);
    }

    @Test
    void eliminaCuandoNoTieneRegistrosRelacionados() {
        Cobrador cobrador = new Cobrador(26);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(26)))
                .thenReturn(0);

        servicio.eliminar(cobrador);

        verify(cobradorService).eliminar(cobrador);
    }
}
