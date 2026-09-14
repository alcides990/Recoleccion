package ama.modulos.egresos;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

final class MapaReporteDatos implements JRDataSource {
    private final Iterator<Map<String, Object>> filas;
    private Map<String, Object> actual;
    MapaReporteDatos(List<Map<String, Object>> filas) { this.filas = filas.iterator(); }
    @Override public boolean next() { if (!filas.hasNext()) return false; actual = filas.next(); return true; }
    @Override public Object getFieldValue(JRField campo) throws JRException {
        if (!actual.containsKey(campo.getName())) throw new JRException("Campo de reporte desconocido: " + campo.getName());
        return actual.get(campo.getName());
    }
}
