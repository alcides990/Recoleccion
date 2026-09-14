package ama.modulos.egresos;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import net.sf.jasperreports.engine.JRException;
import org.springframework.stereotype.Service;

@Service
public class EgresoReporteService {
    private final EgresoService gastos;
    private final EgresoPdfRenderer pdf;

    public EgresoReporteService(EgresoService gastos, EgresoPdfRenderer pdf) {
        this.gastos = gastos;
        this.pdf = pdf;
    }

    public byte[] generar(int sucursal, String nombreSucursal, LocalDate desde, LocalDate hasta)
            throws JRException, IOException {
        return generar(sucursal, nombreSucursal, desde, hasta, TipoReporteEgreso.RESUMIDO);
    }

    public byte[] generar(int sucursal, String nombreSucursal, LocalDate desde, LocalDate hasta, TipoReporteEgreso formato)
            throws JRException, IOException {
        return generar(sucursal, nombreSucursal, desde, hasta, formato, null);
    }

    public byte[] generar(int sucursal, String nombreSucursal, LocalDate desde, LocalDate hasta,
            TipoReporteEgreso formato, Long categoriaId)
            throws JRException, IOException {
        var base = categoriaId == null
                ? gastos.listar(sucursal, desde, hasta)
                : gastos.listar(sucursal, desde, hasta, categoriaId);
        var vigentes = base.stream()
                .filter(gasto -> !gasto.anulado()).toList();
        if (vigentes.isEmpty()) {
            throw new EgresoException(EgresoException.Motivo.SIN_DATOS,
                    "No hay egresos vigentes en el período seleccionado.");
        }
        if (formato == TipoReporteEgreso.RESUMIDO) return pdf.generar(vigentes, nombreSucursal, desde, hasta);
        List<EgresoDto> conDetalles = vigentes.stream().map(gasto -> gastos.detalle(sucursal, gasto.id()))
                .map(gasto -> categoriaId == null ? gasto : gasto.conDetalles(gasto.detalles().stream()
                        .filter(linea -> categoriaId.equals(linea.categoriaId())).toList()))
                .filter(gasto -> !gasto.detalles().isEmpty()).toList();
        return formato == TipoReporteEgreso.DETALLADO
                ? pdf.generarDetallado(conDetalles, nombreSucursal, desde, hasta)
                : pdf.generarPorProducto(conDetalles, nombreSucursal, desde, hasta);
    }
}
