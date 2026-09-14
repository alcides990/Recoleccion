package ama.modulos.ingresosv2;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IngresoComparativoFilaV2 {
    private String serie;
    private Integer periodo;
    private String nombre;
    private Double importe;
    private Long cantidad;
}
