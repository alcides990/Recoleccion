package ama.modulos.rg90;

import java.time.LocalDate;

public record Rg90VentaFila(
        String tipoIdentificacion,
        String numeroIdentificacion,
        String razonSocial,
        String tipoComprobante,
        LocalDate fechaEmision,
        String timbrado,
        String numeroComprobante,
        Long total,
        String condicionVenta) {
}
