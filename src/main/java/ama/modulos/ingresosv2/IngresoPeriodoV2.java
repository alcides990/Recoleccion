package ama.modulos.ingresosv2;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngresoPeriodoV2 {
    private Integer periodo;
    private Double importe;
    private Long cantidad;
}
