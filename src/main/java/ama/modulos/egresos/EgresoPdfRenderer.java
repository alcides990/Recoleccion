package ama.modulos.egresos;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class EgresoPdfRenderer {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(List<EgresoDto> filas, String sucursal, LocalDate desde, LocalDate hasta)
            throws JRException, IOException {
        // Jasper agrega parámetros internos; el mapa debe ser mutable.
        var parametros = new HashMap<String, Object>();
        parametros.put("periodo", desde.format(FECHA) + " al " + hasta.format(FECHA));
        parametros.put("sucursal", sucursal);
        try (var fuente = new ClassPathResource("reportes/egresos.jrxml").getInputStream()) {
            var reporte = JasperCompileManager.compileReport(fuente);
            var impresion = JasperFillManager.fillReport(reporte, parametros, new EgresoReporteDatos(filas));
            return JasperExportManager.exportReportToPdf(impresion);
        }
    }

    public byte[] generarDetallado(List<EgresoDto> gastos, String sucursal, LocalDate desde, LocalDate hasta) throws JRException, IOException {
        var filas = gastos.stream().flatMap(g -> g.detalles().stream().map(l -> Map.<String,Object>of("id", g.id(), "fecha", java.sql.Date.valueOf(g.fecha()), "tipo", g.tipo(), "proveedor", g.proveedor(), "producto", l.producto(), "cantidad", l.cantidad(), "precio", l.precio(), "subtotal", l.cantidad().multiply(l.precio())))).toList();
        return generar("reportes/egresos-detallado.jrxml", filas, sucursal, desde, hasta, "Reporte detallado de egresos");
    }

    public byte[] generarPorProducto(List<EgresoDto> gastos, String sucursal, LocalDate desde, LocalDate hasta) throws JRException, IOException {
        var acumulado = new java.util.TreeMap<String, BigDecimal[]>();
        gastos.forEach(g -> g.detalles().forEach(l -> acumulado.computeIfAbsent(l.producto(), x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO})[0] = acumulado.get(l.producto())[0].add(l.cantidad())));
        gastos.forEach(g -> g.detalles().forEach(l -> { var a = acumulado.get(l.producto()); a[1] = l.precio(); a[2] = a[2].add(l.cantidad().multiply(l.precio())); }));
        var filas = acumulado.entrySet().stream().map(e -> Map.<String,Object>of("producto", e.getKey(), "cantidad", e.getValue()[0], "precio", e.getValue()[1], "subtotal", e.getValue()[2])).toList();
        return generar("reportes/egresos-productos.jrxml", filas, sucursal, desde, hasta, "Egresos agrupados por producto/servicio");
    }

    private byte[] generar(String recurso, List<Map<String, Object>> filas, String sucursal, LocalDate desde, LocalDate hasta, String titulo) throws JRException, IOException {
        var parametros = new HashMap<String, Object>(); parametros.put("periodo", desde.format(FECHA) + " al " + hasta.format(FECHA)); parametros.put("sucursal", sucursal); parametros.put("titulo", titulo);
        try (var fuente = new ClassPathResource(recurso).getInputStream()) { return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(JasperCompileManager.compileReport(fuente), parametros, new MapaReporteDatos(filas))); }
    }
}
