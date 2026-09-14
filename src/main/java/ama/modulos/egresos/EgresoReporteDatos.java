package ama.modulos.egresos;

import java.sql.Date;
import java.util.Iterator;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/** Adapta los DTOs al contrato de campos de Jasper sin acoplar el negocio al motor de PDF. */
public class EgresoReporteDatos implements JRDataSource {
    private final Iterator<EgresoDto> filas;
    private EgresoDto actual;

    public EgresoReporteDatos(List<EgresoDto> datos) {
        filas = datos.iterator();
    }

    @Override
    public boolean next() {
        if (!filas.hasNext()) return false;
        actual = filas.next();
        return true;
    }

    @Override
    public Object getFieldValue(JRField campo) throws JRException {
        return switch (campo.getName()) {
            case "id" -> actual.id();
            case "fecha" -> Date.valueOf(actual.fecha());
            case "tipo" -> actual.tipo();
            case "proveedor" -> actual.proveedor();
            case "comprobante" -> actual.comprobante();
            case "total" -> actual.total();
            default -> throw new JRException("Campo de reporte desconocido: " + campo.getName());
        };
    }
}
