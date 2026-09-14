package ama.modulos.egresos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EgresoDto(
        long id,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate fecha,
        @JsonProperty("tipo_id") long tipoId,
        String tipo, String proveedor, String comprobante, String observacion,
        BigDecimal total, boolean anulado, List<LineaEgreso> detalles) {
    public EgresoDto conDetalles(List<LineaEgreso> lineas) {
        return new EgresoDto(id, fecha, tipoId, tipo, proveedor, comprobante,
                observacion, total, anulado, List.copyOf(lineas));
    }
}
