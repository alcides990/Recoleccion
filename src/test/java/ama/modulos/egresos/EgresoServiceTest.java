package ama.modulos.egresos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EgresoServiceTest {
    private final EgresoRepository repositorio = mock(EgresoRepository.class);
    private final CatalogoService catalogos = mock(CatalogoService.class);
    private final EgresoService servicio = new EgresoService(repositorio, catalogos);

    @Test void calculaCantidadFraccionariaConRedondeoMonetario() {
        assertEquals(new BigDecimal("25.03"), EgresoValidacion.subtotal(
                new LineaEgreso("Combustible", new BigDecimal("2.5"), new BigDecimal("10.01"))));
    }

    @Test void rechazaImportesNegativosYPrecisionExcesiva() {
        assertThrows(EgresoException.class, () -> EgresoValidacion.subtotal(
                new LineaEgreso("Aceite", BigDecimal.ONE, new BigDecimal("-5"))));
        assertThrows(EgresoException.class, () -> EgresoValidacion.subtotal(
                new LineaEgreso("Aceite", new BigDecimal("0.0001"), BigDecimal.ONE)));
    }

    @Test void noEscribeSiAlgunDetalleEsInvalido() {
        var solicitud = solicitud(new LineaEgreso("Nuevo", BigDecimal.ZERO, BigDecimal.TEN));
        assertThrows(EgresoException.class, () -> servicio.guardar(2, 5, null, solicitud));
        verifyNoInteractions(repositorio, catalogos);
    }

    @Test void rechazaTipoDeOtraSucursalAntesDeCrearProveedor() {
        doThrow(EgresoException.datosInvalidos("Tipo inválido")).when(catalogos).validarTipo(2, 6);
        assertThrows(EgresoException.class, () -> servicio.guardar(2, 5, null, solicitud(linea())));
        verifyNoInteractions(repositorio);
        verify(catalogos, never()).resolverNombre(anyInt(), any(), anyString());
    }

    @Test void noModificaGastosAnulados() {
        when(repositorio.bloquearEstado(2, 8)).thenReturn(Optional.of(true));
        assertThrows(EgresoException.class, () -> servicio.guardar(2, 5, 8L, solicitud(linea())));
        verify(repositorio, never()).borrarDetalles(anyInt(), anyLong());
        verify(catalogos, never()).resolverNombre(anyInt(), any(), anyString());
    }

    @Test void rechazaRangoInvertidoSinConsultarBase() {
        assertThrows(EgresoException.class, () -> servicio.listar(2, LocalDate.of(2026,9,8), LocalDate.of(2026,9,1)));
        verifyNoInteractions(repositorio);
    }

    @Test void normalizaLosNombresAntesDeResolverCatalogos() {
        when(catalogos.resolverNombre(2, ClaseCatalogo.PROVEEDOR, "Nuevo proveedor")).thenReturn(3L);
        when(catalogos.resolverNombre(2, ClaseCatalogo.PRODUCTO, "Aceite motor")).thenReturn(4L);
        when(repositorio.crear(eq(2), eq(5), eq(3L), any(), eq(new BigDecimal("10.00")))).thenReturn(7L);
        long id = servicio.guardar(2, 5, null, solicitud(new LineaEgreso("  Aceite   motor ", BigDecimal.ONE, BigDecimal.TEN)));
        assertEquals(7, id);
        verify(repositorio).agregarDetalle(2, 7, 4, new LineaEgreso("Aceite motor", BigDecimal.ONE, BigDecimal.TEN), new BigDecimal("10.00"));
    }

    private LineaEgreso linea() { return new LineaEgreso("Aceite", BigDecimal.ONE, BigDecimal.TEN); }

    private EgresoSolicitud solicitud(LineaEgreso linea) {
        return new EgresoSolicitud(LocalDate.of(2026,9,8), 6L, "  Nuevo   proveedor ", "", "", List.of(linea));
    }
}
